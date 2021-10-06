/*
 * Copyright (C) 2022 Sphereon BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sphereon.ms.eidas.service;

import com.sphereon.ms.auth.jwt.JWTContext;
import com.sphereon.ms.eidas.api.model.certificate.EidasCertificate;
import com.sphereon.ms.eidas.api.model.certificate.EidasCertificateImportRequest;
import com.sphereon.ms.eidas.api.model.signature.CadesSignatureVerifyResponse;
import com.sphereon.ms.eidas.nosql.EidasCertificateRepository;
import com.sphereon.ms.eidas.rest.RestException;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.cades.signature.CMSSignedDocument;
import eu.europa.esig.dss.cades.validation.CAdESCertificateSource;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.DocumentValidator;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class EidasDelegate {
    private final JWTContext jwtContext;
    private final EidasCertificateRepository certificateRepository;

    public EidasDelegate(JWTContext jwtContext, EidasCertificateRepository certificateRepository) {
        this.jwtContext = jwtContext;
        this.certificateRepository = certificateRepository;
    }

    /**
     * Persists a new eidas certificate in the internal database (no HSM)
     *
     * @param certificateImportRequest The certificate in base64 form and name
     * @return The eidas certicate
     * @throws RestException.DuplicateNameException If a certificate with the same name already exists
     */
    public EidasCertificate persistEidasCertificate(EidasCertificateImportRequest certificateImportRequest)
            throws RestException.DuplicateNameException {
        certificateRepository.findByOwnerAndName(getOwner(), certificateImportRequest.getName())
                .ifPresent(certificate -> {
                    throw new RestException.DuplicateNameException(certificate.getName());
                });
        var certificate = new EidasCertificate(getOwner(),
                certificateImportRequest.getName(),
                certificateImportRequest.getBase64Certificate());
        return certificateRepository.save(certificate);
    }

    /**
     * Sign the content using the named certificate, using the provided password
     *
     * @param certificateName The certificate name
     * @param password        The certificate password
     * @param contentBase64   The content to be signed
     * @return The PEM signature object
     */
    public String signWithCertificate(String certificateName, String password, String contentBase64) {
        byte[] p12bytes = getCertificate(certificateName);
        Pkcs12SignatureToken signatureToken = getSignatureToken(p12bytes, password);
        byte[] content = Base64.getDecoder().decode(contentBase64);
        try {
            byte[] signature = signWithPkcs12Token(signatureToken, content);
            return toPem(signature);
        } catch (IOException e) {
            throw new RestException.InvalidSignatureException(String.format("Could not sign content with certificate %s.", certificateName));
        }
    }

    /**
     * Verifies a signature for correctness
     * @param signaturePem
     * @return
     */
    public CadesSignatureVerifyResponse verify(String signaturePem) {
        byte[] signature = fromPem(signaturePem);
        try {
            return verifySignatureBytes(signature);
        } catch (CMSException e) {
            throw new RestException.InvalidSignatureException("Could not verify signature");
        }
    }

    /**
     * Performs the signature validation, includes a simple DSS report.
     *
     * @param signature The signature in bytes
     * @return A simple report and validation information based upon the provided signature
     * @throws CMSException
     */
    private CadesSignatureVerifyResponse verifySignatureBytes(byte[] signature) throws CMSException {
        CMSSignedData cmsSignedData = new CMSSignedData(signature);
        CertificateVerifier cv = getCertificateVerifier(cmsSignedData);
        DSSDocument document = new CMSSignedDocument(cmsSignedData);
        SignedDocumentValidator documentValidator = SignedDocumentValidator.fromDocument(document);
        documentValidator.setCertificateVerifier(cv);
        Reports reports = documentValidator.validateDocument();
        SimpleReport simpleReport = reports.getSimpleReport();
        String originalData = getSignedData(documentValidator);
        return new CadesSignatureVerifyResponse(simpleReport.isValid(simpleReport.getFirstSignatureId()),
                originalData, simpleReport.getJaxbModel());
    }

    /**
     * Signs the input content with a PKCS12 signature token
     *
     * @param token
     * @param content
     * @return
     * @throws IOException
     */
    private byte[] signWithPkcs12Token(Pkcs12SignatureToken token, byte[] content) throws IOException {
        var toSignDocument = new InMemoryDocument(content);
        DSSPrivateKeyEntry privateKey = token.getKeys().get(0);
        CAdESSignatureParameters parameters = construcCadesBSignatureParameters(privateKey);
        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();
        CAdESService service = new CAdESService(commonCertificateVerifier);
        ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
        DigestAlgorithm digestAlgorithm = parameters.getDigestAlgorithm();
        SignatureValue signatureValue = token.sign(dataToSign, digestAlgorithm, privateKey);
        CMSSignedDocument signedDocument = (CMSSignedDocument) service.signDocument(toSignDocument, parameters, signatureValue);
        return signedDocument.getCMSSignedData().getEncoded();
    }

    private CAdESSignatureParameters construcCadesBSignatureParameters(DSSPrivateKeyEntry privateKey) {
        CAdESSignatureParameters parameters = new CAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
        parameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
        parameters.setSigningCertificate(privateKey.getCertificate());
        parameters.setCertificateChain(privateKey.getCertificateChain());
        return parameters;
    }

    private SignerInformation getSignerInformation(CMSSignedData cmsSignedData) {
        return new ArrayList<>(cmsSignedData.getSignerInfos().getSigners()).get(0);
    }

    private CertificateVerifier getCertificateVerifier(CMSSignedData cmsSignedData) {
        SignerInformation signerInformation = getSignerInformation(cmsSignedData);
        CertificateVerifier cv = new CommonCertificateVerifier();
        cv.setDataLoader(new CommonsDataLoader());
        cv.setOcspSource(new OnlineOCSPSource());
        cv.setCrlSource(new OnlineCRLSource());
        CommonTrustedCertificateSource trustedCertSource = new CommonTrustedCertificateSource();
        // todo: POC. This allows all certificates (should be update before production)
        CertificateSource certificateSource = new CAdESCertificateSource(cmsSignedData, signerInformation);
        trustedCertSource.importAsTrusted(certificateSource);
        cv.addTrustedCertSources(trustedCertSource);
        return cv;
    }

    private Pkcs12SignatureToken getSignatureToken(byte[] p12bytes, String password) {
        return new Pkcs12SignatureToken(p12bytes,
                new KeyStore.PasswordProtection(password.toCharArray()));
    }

    private byte[] getCertificate(String name) {
        EidasCertificate eidasCertificate = certificateRepository.findByOwnerAndName(getOwner(), name)
                .orElseThrow(() -> new RestException.InvalidNameException(name));
        return Base64.getDecoder().decode(eidasCertificate.getBase64CertificateString());
    }


    /**
     * Get the document from the input
     *
     * @param documentValidator
     * @return
     */
    private String getSignedData(DocumentValidator documentValidator) {
        List<AdvancedSignature> signatures = documentValidator.getSignatures();
        // todo: POC assumption
        AdvancedSignature advancedSignature = signatures.get(0);
        List<DSSDocument> originalDocuments = documentValidator.getOriginalDocuments(advancedSignature.getId());
        // todo: POC assumption
        InMemoryDocument original = (InMemoryDocument) originalDocuments.get(0);
        return original.getBase64Encoded();
    }


    /**
     * Create a PEM string from the input bytes
     *
     * @param cms
     * @return
     * @throws IOException
     */
    private String toPem(byte[] cms) throws IOException {
        try (StringWriter sw = new StringWriter(); JcaPEMWriter writer = new JcaPEMWriter(sw)) {
            ContentInfo ci = ContentInfo.getInstance(ASN1Sequence.fromByteArray(cms));
            writer.writeObject(ci);
            writer.close();
            return sw.toString();
        }
    }

    /**
     * Returns the bytes from a PEM string
     *
     * @param pem
     * @return
     */
    private byte[] fromPem(String pem) {
        String base64 = pem.replace("-----BEGIN PKCS7-----", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace("-----END PKCS7-----", "");
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Get tenant info from the JWT claim
     *
     * @return
     */
    private String getOwner() {
        return jwtContext.claimAccess().getSphereonIdentifier(Optional.empty(), Optional.empty());
    }
}
