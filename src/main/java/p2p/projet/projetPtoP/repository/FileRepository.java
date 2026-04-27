package p2p.projet.projetPtoP.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import p2p.projet.projetPtoP.entity.Fichier;

public interface FileRepository extends JpaRepository<Fichier, Long>{

}
