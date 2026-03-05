package com.fiscalimplify.fiscalimplify.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade responsável por armazenar os dados das empresas cadastradas no sistema
 * e na Nuvem Fiscal (CNPJ, razão social, IE, UF, município, CRT).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Entity
@Table(name = "empresas", indexes = {
        @Index(name = "idx_empresas_cnpj", columnList = "cnpj"),
        @Index(name = "idx_empresas_razao_social", columnList = "razao_social"),
        @Index(name = "idx_empresas_nome_fantasia", columnList = "nome_fantasia"),
        @Index(name = "idx_empresas_inscricao_estadual", columnList = "inscricao_estadual"),
        @Index(name = "idx_empresas_uf", columnList = "uf"),
        @Index(name = "idx_empresas_codigo_municipio", columnList = "codigo_municipio"),
        @Index(name = "idx_empresas_crt", columnList = "crt"),
        @Index(name = "idx_empresas_registrada_nuvem", columnList = "registrada_nuvem"),
        @Index(name = "idx_empresas_criado_em", columnList = "criado_em"),
        @Index(name = "idx_empresas_atualizado_em", columnList = "atualizado_em")
})
@Getter
@Setter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(nullable = false, name = "razao_social", length = 200)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;

    @Column(name = "inscricao_estadual", length = 14)
    private String inscricaoEstadual;

    @Column(length = 2)
    private String uf;

    @Column(name = "codigo_municipio", length = 7)
    private String codigoMunicipio;

    @Column(name = "nome_municipio", length = 100)
    private String nomeMunicipio;

    @Column(name = "crt")
    private Integer crt;

    @Column(nullable = false, name = "registrada_nuvem")
    private Boolean registradaNuvem = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = Instant.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = Instant.now();
    }
}
