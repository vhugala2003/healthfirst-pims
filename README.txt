HEALTHFIRST PHARMACY INVENTORY MANAGEMENT SYSTEM
=================================================

Demo login accounts
  Administrator: admin / admin123
  Cashier:       cashier / cash123

Change these demo passwords before any use beyond local assessment.

SETUP
  1. Install Java 17+, Maven 3.9+, and MySQL 8 (or Docker Desktop).
  2. From this folder, start sample MySQL with: docker compose up -d
  3. Set DB_URL, DB_USER, and DB_PASSWORD as described in README.md.
  4. Build with: mvn clean package
  5. Run with: java -jar target/healthfirst-pims.jar

The Windows installer can be created with scripts\build-windows.ps1 on
Windows with a JDK containing jpackage and WiX Toolset 3.0 installed.
The MySQL server is a separate dependency and is not bundled in the installer.

ADMIN FEATURES
  Medicine catalogue, supplier directory, cashier account management,
  Sales report, Item-wise report, Low stock report, and Expiry report.

CASHIER FEATURES
  Point of sale, stock lookup, transactional checkout, and receipt save/print.

This application is an educational demonstration. Sample contact information
is fictional. Review security, licensing, privacy, backups, and compliance
before any real pharmacy deployment.