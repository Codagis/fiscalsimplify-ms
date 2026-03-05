package com.fiscalimplify.fiscalimplify.domain.repository;

import com.fiscalimplify.fiscalimplify.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA responsável pelo acesso a dados da entidade Company (empresas).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}
