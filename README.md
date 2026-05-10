# TP1 CORBA — Service PDF Distribué

Application CORBA de manipulation de PDFs avec interface web, déployée via Docker.

## Fonctionnalités
- Créer un PDF
- Fusionner deux PDFs
- Découper un PDF
- Supprimer une page
- Protéger par mot de passe
- Convertir une page en image
- Extraire le texte

## Lien de démonstration
corba-tp1-production.up.railway.app

## Technologies
- Java 1.8 + CORBA
- Apache PDFBox 2.0.30
- Docker & Docker Compose

## Lancer en local
```bash
docker compose up --build
```
Puis ouvrir http://localhost:8080

## Auteur
Ahmad Dame — Master MaDSI — USSEIN
