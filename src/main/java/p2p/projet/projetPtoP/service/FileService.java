package p2p.projet.projetPtoP.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import p2p.projet.projetPtoP.config.NodeConfig;
import p2p.projet.projetPtoP.entity.Fichier;
import p2p.projet.projetPtoP.repository.FileRepository;

import java.util.List;

@Service
public class FileService {
    @Autowired(required=false)
    private NodeConfig config;
    @Autowired(required=false)
    private FileRepository fileRepository;
 
    Fichier fichier = new Fichier();

    // TODO 1: Sauvegarder le fichier localement

    public void saveFile(String filename, byte[] data) {
        fichier.setNomFichier(filename);
        fichier.setFichier(data);
        fileRepository.save(fichier);
        //replicateFile(filename, data);
    }

    // TODO 2: Lire un fichier local
    public byte[] getFile(String filename) {
        // rechercher un fichier par son nom et retourner son fichier
        fichier = fileRepository.findByNomFichier(filename);
        byte[] fichierTrouver = fichier.getFichier();
        return fichierTrouver;
        //return null;
    }

    // TODO 3: Ajouter la réplication
    private void replicateFile(String filename, byte[] data) {
        // À implémenter
    }

    // TODO 4: Recherche distribuée
    private byte[] searchInPeers(String filename) {
        // À implémenter
        return null;
    }

}
