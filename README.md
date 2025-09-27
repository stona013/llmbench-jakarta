# llmbench-jakarta — LLM Benchmark Cockpit (Jakarta EE + Docker)

Ein prototypisches Benchmark-Cockpit für Large Language Models (LLMs) mit Fokus auf Vergleichbarkeit und Parametrisierung über ein Jakarta-basiertes Web-Frontend. Die Anwendung verwendet Ollama als lokale LLM-API und erlaubt Benchmarks wie Cold/Warm-Start, Konsistenztests und parallele Anfragen.


## Funktionen

- Weboberfläche für Modell- und Prompt-Benchmarks
- Unterstützung mehrerer Benchmark-Pläne:
  - **Cold vs Warm**: Initialer vs. wiederholter Aufruf
  - **Konsistenz**: Stabilität der Antworten über mehrere Läufe
  - **Parallelität**: Lastverhalten über gleichzeitige Anfragen
- Keyword-basierter Qualitätscheck (Trefferquote)
- Tokenzählung, Antwortzeiten, HTTP-Status, Erfolgsanzeige
- CSV-Export aller Ergebnisse
- Ollama-Modellliste via /api/models
- GUI vollständig clientseitig (HTML, JavaScript)


## Voraussetzungen

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Ollama (wird via Docker-Compose mitgestartet)


## Projektstruktur

```
llmbench-jakarta/
├── backend/                # Jakarta EE Anwendung (REST)
│   └── src/main/java/…     # Services, DTOs, API-Handler
├── src/main/webapp/        # index.html + JavaScript GUI
├── Dockerfile              # Backend-Image (WildFly)
├── docker-compose.yml      # ollama + backend
└── README.md
```


## Setup

### 1. Repository klonen

```bash
git clone https://github.com/stona013/llmbench-jakarta.git
cd llmbench-jakarta
```

### 2. Backend bauen

```bash
mvn clean package
```

### 3. Anwendung starten

```bash
docker compose up --build
```

### 4. Zugriff auf das UI

```http
http://localhost:8080/
```

### GUI-Funktionen

- Testplan-Auswahl (z. B. Konsistenz, Parallelität)
- Qualität: Prozentuale Keyword-Abdeckung
- CSV-Export aller Läufe
- Rohdaten-Log in JSON zur Analyse


## Metriken

| Feld             | Bedeutung                                 |
|------------------|--------------------------------------------|
| Dauer ms         | Zeit vom Request bis zur Antwort           |
| Tokens in / out  | Eingabe- und Ausgabetokens                 |
| Quality          | Anteil der Keywords in der Antwort         |
| Fehler           | Exception / Timeout / Status != 200        |


## Ollama einrichten
Ollama ist eine lokale LLM-Laufzeitumgebung. Dieses Projekt verwendet es als lokalen API-Endpunkt.
Die Konfiguration erfolgt automatisch über docker-compose.yml, aber hier die wichtigsten Schritte:

Modelle werden zur Laufzeit automatisch heruntergeladen, sofern sie nicht lokal verfügbar sind. Alternativ kannst du sie manuell vorab laden:

```
ollama pull qwen:1.8b
```
```
ollama pull mistral
```
```
ollama pull llama2
```

# Weitere Modelle finden
Du findest zusätzliche Modelle in den offiziellen Bibliotheken bzw. Repositorien von Ollama:
Ollama Model Library — offizielle Liste verfügbarer Modelle
```
https://ollama.com/models
```
Hugging Face Model Hub — viele Modelle, die für LLM-Inference geeignet sind
```
https://huggingface.co/models
```
Je nach model:-Eintrag im GUI (bzw. API) können mehrere Varianten unterstützt werden:
```
ollama pull qwen2.5:3b
```
```
ollama pull gemma:2b
```
