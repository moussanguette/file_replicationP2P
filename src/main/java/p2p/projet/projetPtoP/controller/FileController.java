package p2p.projet.projetPtoP.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import p2p.projet.projetPtoP.service.FileService;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;

    // Upload principal : sauvegarde locale + réplication
    @PostMapping("/{filename}")
    public ResponseEntity<String> upload(
            @PathVariable String filename,
            @RequestBody byte[] data) {
        fileService.saveFile(filename, data);
        return ResponseEntity.ok("Fichier '" + filename + "' uploadé et répliqué.");
    }

    // Endpoint interne utilisé uniquement par la réplication (pas de re-réplication)
    @PostMapping("/replicate/{filename}")
    public ResponseEntity<String> replicate(
            @PathVariable String filename,
            @RequestBody byte[] data) {
        fileService.saveLocalOnly(filename, data);
        return ResponseEntity.ok("Fichier '" + filename + "' répliqué localement.");
    }

    // Download : local d'abord, puis recherche sur les peers
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> download(@PathVariable String filename) {
        byte[] data = fileService.getFile(filename);
        if (data == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(data);
    }
}
