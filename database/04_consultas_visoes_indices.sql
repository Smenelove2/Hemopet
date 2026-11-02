USE hemopet;

-- Indices para acelera a busca de bolsas por status e tipo, usado na view de estoque
CREATE INDEX idx_bolsa_status_tipo
ON  bolsa_hemocomponente(status_bolsa, data_validade);

-- Indice para acelerar a filtragem de soliçitacoes por urgencia e data, usado em relatorio
CREATE INDEX idx_solicitacao_urgencia_data
ON solicitacao(urgencia, data_solicitacao);

--ANTI JOIN, lista os tutores que ainda não possuem nenhum animal doador vinculado
SELECT p.cpf,
    p.nome,
    p.cidade
FROM tutor t
JOIN pessoa p ON t.cpf = p.cpf
LEFT JOIN animal_doador a ON t.cpf = a.cpf_tutor
WHERE a.id_animal IS NULL;

--FULL OUTER JOIN
-- Cruzamento completo entre hospitais veterinarios e solicitaçoes
SELECT h.nome_fantasia,
       h.cnpj,
       s.id_solicitacao,
       s.urgencia,
       s.status_solicitacao
FROM hospital_veterinario h
LEFT JOIN solicitacao S ON h.id_hospital = s.id_hospital
UNION
SELECT 
    h.nome_fantasia,
    h.cnpj,
    s.id_solicitacao,
    s.urgencia,
    s.status_solicitacao
FROM hospital_veterinario h
RIGHT JOIN solicitacao s ON h.id_hospital = s.id_hospital;

--Subconsulta
--Buscar coletas que tiveram volume coletado acima da media geral
SELECT 
    c.id_coleta,
    c.data_hora,
    c.volume_ml_total,
    a.nome AS nome_animal,
    a.especie
FROM coleta c
JOIN animal_doador a ON c.id_animal = a.id_animal
WHERE c.volume_ml_total > (
    SELECT AVG(volume_ml_total) 
    FROM coleta
);

--Subconsulta 2
-- Listar os veterinarios que ja realizaram/aprovaram pelo menos uma coleta.
SELECT 
    p.nome AS nome_veterinario,
    v.crmv
FROM veterinario v
JOIN pessoa p ON v.cpf = p.cpf
WHERE EXISTS (
    SELECT 1 
    FROM coleta c 
    WHERE c.cpf_veterinario = v.cpf 
      AND c.status_aprovacao = 'APROVADA'
);

--VIEWS
--Visao 1: painel completo de rastreabilidade de doaçoes (Relação Tutor -> Animal -> Coleta -> Bolsa -> Vet)
-- Justificativa Semântica: Permite à equipe de auditoria rastrear rapidamente a origem de cada bolsa de sangue, 
-- desde o tutor e o animal doador até o veterinário que realizou a coleta.
CREATE OR REPLACE VIEW vw_rastreabilidade_doacao AS
SELECT 
    b.id_bolsa,
    b.tipo_componente,
    b.status_bolsa,
    b.data_validade,
    c.id_coleta,
    c.data_hora AS data_coleta,
    a.nome AS nome_animal,
    a.especie,
    p_tutor.nome AS nome_tutor,
    p_vet.nome AS nome_veterinario,
    v.crmv
FROM bolsa_hemocomponente b
JOIN coleta c ON b.id_coleta = c.id_coleta
JOIN animal_doador a ON c.id_animal = a.id_animal
JOIN tutor t ON a.cpf_tutor = t.cpf
JOIN pessoa p_tutor ON t.cpf = p_tutor.cpf
JOIN veterinario v ON c.cpf_veterinario = v.cpf
JOIN pessoa p_vet ON v.cpf = p_vet.cpf;

-- Visão 2: Resumo de Atendimentos de Solicitações aos Hospitais (Hospital -> Solicitação -> Item -> Bolsa)
-- Justificativa Semântica: Agrupa as demandas ativas dos hospitais parceiros, detalhando quais bolsas e 
-- quantidades foram alocadas para cada item solicitado.
CREATE OR REPLACE VIEW vw_resumo_solicitacoes_hospital AS
SELECT 
    h.nome_fantasia AS hospital,
    h.cnpj,
    s.id_solicitacao,
    s.urgencia,
    s.status_solicitacao,
    its.id_item,
    its.quantidade,
    its.ht_percentual,
    b.id_bolsa,
    b.tipo_componente
FROM hospital_veterinario h
JOIN solicitacao s ON h.id_hospital = s.id_hospital
JOIN item_solicitacao its ON s.id_solicitacao = its.id_solicitacao
JOIN bolsa_hemocomponente b ON its.id_bolsa = b.id_bolsa;