package p2p.projet.projetPtoP.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

@Entity
@Data
public class Fichier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nomFichier;
	
    @Lob
    private byte[] fichier;

}
