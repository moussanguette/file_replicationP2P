package p2p.projet.projetPtoP.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import p2p.projet.projetPtoP.config.NodeConfig;
import p2p.projet.projetPtoP.entity.Fichier;
import p2p.projet.projetPtoP.repository.FileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private NodeConfig config;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Sauvegarde locale + réplication vers les peers
    public void saveFile(String filename, byte[] data) {
        saveLocalOnly(filename, data);
        replicateFile(filename, data);
    }

    // Sauvegarde locale uniquement (appelée par l'endpoint /replicate pour éviter la boucle infinie)
    public void saveLocalOnly(String filename, byte[] data) {
        if (fileRepository.findByNomFichier(filename) != null) {
            log.info("[LOCAL] Fichier '{}' déjà présent, ignoré.", filename);
            return;
        }
        Fichier fichier = new Fichier();
        fichier.setNomFichier(filename);
        fichier.setFichier(data);
        fileRepository.save(fichier);
        log.info("[LOCAL] Fichier '{}' sauvegardé ({} octets).", filename, data.length);
    }

    // Lecture locale, puis recherche sur les peers si absent
    public byte[] getFile(String filename) {
        Fichier fichier = fileRepository.findByNomFichier(filename);
        if (fichier != null) {
            log.info("[LOCAL] Fichier '{}' trouvé localement.", filename);
            return fichier.getFichier();
        }
        log.info("[LOCAL] Fichier '{}' absent localement, recherche sur les peers...", filename);
        return searchInPeers(filename);
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
                    log.info("[SEARCH] Fichier '{}' trouvé sur le peer {}.", filename, peer);
                    return response.getBody();
                }
            } catch (Exception e) {
                log.warn("[SEARCH] Peer {} injoignable ou fichier absent : {}", peer, e.getMessage());
            }
        }
        log.warn("[SEARCH] Fichier '{}' introuvable sur tous les peers.", filename);
        return null;
    }
}
