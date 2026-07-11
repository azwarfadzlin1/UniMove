# Security Policy

Please report suspected vulnerabilities privately to the repository owner instead of opening a public issue.

## Secrets

This repository must never contain API credentials, signing passwords, keystores, service-account files, `local.properties`, or `google-services.json`. If a credential is committed, revoke or rotate it immediately and remove it from Git history before publishing.

Firebase client configuration identifies a Firebase project but does not authorize database access by itself. Firestore Security Rules, Firebase Authentication, App Check, API restrictions, and quota monitoring remain mandatory.

## Supported scope

Security fixes target the latest `main` branch. Location disclosure, authorization bypass, insecure exported Android components, credential exposure, and Firestore rule bypasses are treated as high priority.
