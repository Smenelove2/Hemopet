USE hemopet;

-- 1) Quantidade de animais por especie.
SELECT especie, COUNT(*) AS quantidade_animais_por_especie
FROM animal_doador
GROUP BY especie
ORDER BY especie ASC;

-- 2) Animais e seus respectivos tutores em ordem alfabetica.
SELECT p.nome AS tutor, a.nome AS animal
FROM pessoa p
JOIN tutor t
    ON p.cpf = t.cpf
JOIN animal_doador a
    ON a.cpf_tutor = t.cpf
ORDER BY p.nome ASC, a.nome ASC;

-- 3) Animais acima da media de peso geral e seus tutores.
SELECT a.nome AS animal, p.nome AS tutor, a.peso
FROM pessoa p
JOIN tutor t
    ON p.cpf = t.cpf
JOIN animal_doador a
    ON a.cpf_tutor = t.cpf
WHERE a.peso > (
    SELECT AVG(peso)
    FROM animal_doador
)
ORDER BY a.peso DESC;

-- 4) Tutores que possuem pelo menos um cao e um gato.
SELECT p.nome AS tutor, SUM(a.especie = 'CAO') AS quantidade_caes, SUM(a.especie = 'GATO') AS quantidade_gatos
FROM pessoa p
JOIN tutor t
    ON p.cpf = t.cpf
JOIN animal_doador a
    ON a.cpf_tutor = t.cpf
GROUP BY p.cpf, p.nome
HAVING SUM(a.especie = 'CAO') > 0
   AND SUM(a.especie = 'GATO') > 0
ORDER BY p.nome ASC;
