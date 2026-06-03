package ru.meowmure.auth.dto;

public class PublicKeyResponse {

    private String publicKey;
    private String algorithm;

    public PublicKeyResponse() {
    }

    public PublicKeyResponse(String publicKey, String algorithm) {
        this.publicKey = publicKey;
        this.algorithm = algorithm;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
