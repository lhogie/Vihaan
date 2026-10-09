package fr.lab.hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.lab.hr.model.Poste;

public interface PosteRepository extends JpaRepository<Poste, Long>, JpaSpecificationExecutor<Poste> {
}
