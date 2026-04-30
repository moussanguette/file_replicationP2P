package p2p.projet.projetPtoP.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import p2p.projet.projetPtoP.config.NodeConfig;
import p2p.projet.projetPtoP.service.FileService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/node")
public class NodeController {

    @Autowired
    private NodeConfig config;

    @Autowired
    private FileService fileService;

    @Value("${server.port}")
    private int port;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("port", port);
        info.put("storage", config.getStorage());
        info.put("peers", config.getPeers());
        info.put("fichiers_locaux", fileService.listFiles());
        return ResponseEntity.ok(info);
    }
}
