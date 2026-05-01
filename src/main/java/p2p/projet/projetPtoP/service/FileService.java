package p2p.projet.projetPtoP.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import p2p.projet.projetPtoP.config.NodeConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private NodeConfig config;

    @Autowired
    private RestTemplate restTemplate;

    // Sauvegarde locale + réplication vers les peers
    public void saveFile(String filename, byte[] data) {
        saveLocalOnly(filename, data);
        replicateFile(filename, data);
    }

    // Sauvegarde locale uniquement (appelée par /replicate pour éviter la boucle infinie)
    public void saveLocalOnly(String filename, byte[] data) {
        try {
            Path storageDir = Path.of(config.getStorage());
            Files.createDirectories(storageDir);
            Path filePath = storageDir.resolve(filename);
            if (Files.exists(filePath)) {
                log.info("[LOCAL] Fichier '{}' déjà présent, ignoré.", filename);
                return;
            }
            Files.write(filePath, data);
            log.info("[LOCAL] Fichier '{}' sauvegardé ({} octets).", filename, data.length);
        } catch (IOException e) {
            throw new RuntimeException("Erreur sauvegarde : " + filename, e);
        }
    }

    // Lecture locale, puis recherche sur les peers si absent
    public byte[] getFile(String filename) {
        Path filePath = Path.of(config.getStorage(), filename);
        if (Files.exists(filePath)) {
            try {
                log.info("[LOCAL] Fichier '{}' trouvé localement.", filename);
                return Files.readAllBytes(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Erreur lecture : " + filename, e);
            }
        }
        log.info("[LOCAL] Fichier '{}' absent, recherche sur les peers...", filename);
        return searchInPeers(filename);
    }

    // Liste les fichiers stockés localement
    public List<String> listFiles() {
        try {
            Path storageDir = Path.of(config.getStorage());
            if (!Files.exists(storageDir)) return Collections.emptyList();
            return Files.list(storageDir)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    // Réplication : envoi du fichier à chaque peer connu
    private void replicateFile(String filename, byte[] data) {
        for (String peer : config.getPeers()) {
            try {
                String url = peer + "/files/replicate/" + filename;
                restTemplate.postForEntity(url, data, String.class);
                log.info("[REPLICATION] Fichier '{}' répliqué vers {}.", filename, peer);
            } catch (Exception e) {
                log.warn("[REPLICATION] Peer {} injoignable : {}", peer, e.getMessage());
            }
        }
    }

    // Recherche distribuée : interroge chaque peer jusqu'à trouver le fichier
    private byte[] searchInPeers(String filename) {
        for (String peer : config.getPeers()) {
            try {
                String url = peer + "/files/" + filename;
                ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("[SEARCH] Fichier '{}' trouvé sur {}.", filename, peer);
                    return response.getBody();
                }
            } catch (Exception e) {
                log.warn("[SEARCH] Peer {} injoignable : {}", peer, e.getMessage());
            }
        }
        log.warn("[SEARCH] Fichier '{}' introuvable sur tous les peers.", filename);
        return null;
    }
}
