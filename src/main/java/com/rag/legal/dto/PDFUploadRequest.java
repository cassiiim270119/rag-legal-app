package com.rag.legal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de upload de PDF
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PDFUploadRequest {
    
    /**
     * Tribunal responsável pelo documento
     * Exemplos: STF, STJ, TRF, TJSP, etc
     */
    private String tribunal;
    
    /**
     * Área legal do documento
     * Exemplos: PENAL, CIVIL, TRABALHISTA, ADMINISTRATIVO, etc
     */
    private String legalArea;
    
    /**
     * Tipo de documento
     * Exemplos: SUMULA, LEI, DECRETO, RESOLUCAO, etc
     */
    private String documentType;
    
    /**
     * Descrição adicional do documento
     */
    private String description;
}
