# Simulador de Redes de Filas G/G/c/K (M4-SMA)

Simulador de eventos discretos para redes de filas com **topologia arbitraria**,
carregada de um arquivo `.yml` no mesmo formato do simulador do módulo 3.
Escrito em Java (JDK 11+), **sem dependências externas** (o YAML é lido por um parser próprio).

## Como compilar e executar

```bash
javac *.java
java Simulador model.yml
java Simulador model.yml -v
```

O primeiro comando mostra o resultado (média, se houver várias sementes); com `-v`
mostra também o resultado de cada semente. Exemplo (modelo do T1):
`java Simulador exemplos/modelo-t1.yml`.

Sem argumento, o programa procura `model.yml` na pasta atual.

## Arquivos

| Arquivo | Função |
|---|---|
| `Simulador.java` | Escalonador de eventos, roteamento, relatório e `main` |
| `Modelo.java` | Carrega e valida o `.yml` |
| `YamlSimples.java` | Leitor mínimo de YAML (mapas, listas, comentários, `!PARAMETERS`) |
| `Fila.java` | Fila G/G/c/K (capacidade omitida = infinita) |
| `Gerador.java` | Números aleatórios: gerador congruencial (semente) ou lista fixa |
| `Evento.java`, `TipoEvento.java` | Eventos de CHEGADA / SAIDA |
| `model.yml` | Modelo de exemplo pronto para rodar |
| `exemplos/` | Variantes: com `seeds`, com `rndnumbers` e o `model` original (incompleto) |

## Formato do arquivo

```yaml
!PARAMETERS
arrivals:            # fila: instante da 1a chegada externa
   Q1: 2.0

queues:
   Q1:
      servers: 2        # obrigatorio
      capacity: 4       # opcional (omitido = infinita)
      minArrival: 1.0   # obrigatorio para filas em 'arrivals'
      maxArrival: 4.0
      minService: 1.0   # obrigatorio
      maxService: 1.5

network:             # probabilidade de ir de source para target
-  source: Q1
   target: Q2
   probability: 0.78 # o que sobra (1 - soma) e saida do sistema

seeds:               # usa gerador congruencial, uma execucao por semente
- 1
- 2
rndnumbersPerSeed: 100000
# OU, em vez de seeds (a simulacao termina quando a lista acaba):
# rndnumbers:
# - 0.2176
# - 0.0103
```

Regras: se `seeds` existir, `rndnumbers` é ignorado. Chegadas e atendimentos seguem
distribuição uniforme em `[min, max]`. Erros no arquivo (parâmetro faltando, fila
inexistente, probabilidades somando > 1...) são listados todos de uma vez antes de rodar.

## Saída

Para cada fila: tempo global, clientes perdidos e, por estado (nº de clientes),
tempo acumulado e probabilidade. Com várias sementes, exibe a **média** das execuções.

## Decisões de implementação (conferir com o simulador do módulo 3)

- Ordem de consumo dos aleatórios: (chegada) próxima chegada → tempo de atendimento;
  (saída) tempo do próximo da fila → sorteio do destino → atendimento no destino.
- Fila **sem rotas de saída** não consome número aleatório no sorteio de destino.
- Eventos no mesmo instante são processados na ordem em que foram agendados.
- O `model.yml` recebido do módulo 3 não define `minArrival/maxArrival/minService/maxService`
  para Q1; por isso `model.yml` aqui traz valores de exemplo (1.0–4.0 e 1.0–1.5) para ela.
