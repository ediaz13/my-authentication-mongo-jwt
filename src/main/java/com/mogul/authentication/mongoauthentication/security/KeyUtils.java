package com.mogul.authentication.mongoauthentication.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Objects;

@Component
public class KeyUtils {

    @Autowired
    Environment environment;

    @Value("access-token.private")
    private String accessTokenPrivateKeyPath;

    @Value("access-token.public")
    private String accessTokenPublicKeyPath;

    @Value("refresh-token.private")
    private String refreshTokenPrivateKeyPath;

    @Value("refresh-token.public")
    private String refreshTokenPublicKeyPath;

    private KeyPair _accessTokenKeyPair;
    private KeyPair _refreshTokenKeyPair;

    public KeyPair getAccessTokenKeyPair() {
        if (Objects.isNull(_accessTokenKeyPair)) {
            _accessTokenKeyPair = getKeyPair(accessTokenPublicKeyPath, refreshTokenPrivateKeyPath);
        }

        return _accessTokenKeyPair;
    }

    private KeyPair get_refreshTokenKeyPair() {
        if(Objects.isNull(_refreshTokenKeyPair)) {
            _refreshTokenKeyPair = getKeyPair(refreshTokenPublicKeyPath, refreshTokenPrivateKeyPath);
        }
        return _refreshTokenKeyPair;
    }

    private KeyPair getKeyPair(String publicKeyPath, String privateKeyPath) {
        KeyPair keyPair;

        File publicKeyFile = new File(publicKeyPath);
        File privateKeyFile = new File(privateKeyPath);

        if (publicKeyFile.exists() && privateKeyFile.exists()) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");

                byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
                EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

                byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
                EncodedKeySpec privateKeySpec = new X509EncodedKeySpec(privateKeyBytes);
                PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

                keyPair = new KeyPair(publicKey, privateKey);
                return keyPair;
            } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
                throw new RuntimeException("Failed to read key pair", e);
            }

        } else {
            // Generate the key pair
            if (Arrays.stream(environment.getActiveProfiles()).anyMatch(env -> env.equals("prod"))) {
                throw new RuntimeException("Key pair not found");
            }
        }

        File directory = new File("access-refresh-token-keys");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
            try (FileOutputStream fos = new FileOutputStream(publicKeyPath)) {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
                fos.write(keySpec.getEncoded());
            }

            try (FileOutputStream fos = new FileOutputStream(privateKeyPath)) {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
                fos.write(keySpec.getEncoded());
            }

            return keyPair;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }

    public RSAPublicKey getAccessTokenPublicKey() {
        return (RSAPublicKey) getAccessTokenKeyPair().getPublic();
    }

    public RSAPublicKey getAccessTokenPrivateKey() {
        return (RSAPublicKey) getAccessTokenKeyPair().getPrivate();
    }

    public RSAPublicKey getRefreshTokenPublicKey() {
        return (RSAPublicKey) get_refreshTokenKeyPair().getPublic();
    }

    public RSAPublicKey getRefreshTokenPrivateKey() {
        return (RSAPublicKey) get_refreshTokenKeyPair().getPrivate();
    }
}
