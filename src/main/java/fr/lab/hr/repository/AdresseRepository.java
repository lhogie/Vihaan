package fr.lab.hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.lab.hr.model.Adresse;

public interface AdresseRepository extends JpaRepository<Adresse, Long>, JpaSpecificationExecutor<Adresse> {
}
