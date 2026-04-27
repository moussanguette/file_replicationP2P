package p2p.projet.projetPtoP.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
    @PostMapping("/{filename}")
    public ResponseEntity<String> upload(
    		
            @PathVariable String filename,
            @RequestBody byte[] data) {

        fileService.saveFile(filename, data);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> download(@PathVariable String filename) {
        byte[] data = fileService.getFile(filename);
        return ResponseEntity.ok(data);
    }



}
