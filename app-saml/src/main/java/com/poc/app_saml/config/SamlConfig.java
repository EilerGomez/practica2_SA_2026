package com.poc.app_saml.config;

import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;

@Configuration
public class SamlConfig {

    @Value("${saml.metadata-uri}")
    private String metadataUri;

    @Value("${saml.registration-id}")
    private String registrationId;

    @Value("${saml.entity-id}")
    private String entityId;

    @Value("${saml.acs-location}")
    private String acsLocation;

    @Value("${saml.private-key-location}")
    private String privateKeyLocation;

    @Value("${saml.certificate-location}")
    private String certificateLocation;

    @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() throws Exception {

        Saml2X509Credential signingCredential =
            Saml2X509Credential.signing(loadPrivateKey(), loadCertificate());

        RelyingPartyRegistration registration = RelyingPartyRegistrations
            .fromMetadataLocation(metadataUri)
            .registrationId(registrationId)
            .entityId(entityId)
            .assertionConsumerServiceLocation(acsLocation)
            .signingX509Credentials(credentials -> credentials.add(signingCredential))
            .build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    private PrivateKey loadPrivateKey() throws Exception {
        try (InputStream stream = new ClassPathResource(privateKeyLocation).getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(stream);
        }
    }

    private X509Certificate loadCertificate() throws Exception {
        try (InputStream stream = new ClassPathResource(certificateLocation).getInputStream()) {
            return (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(stream);
        }
    }
}