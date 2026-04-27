package p2p.projet.projetPtoP.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "node")
@Data

public class NodeConfig {
    private String storage;
    private List<String> peers;
    

}
