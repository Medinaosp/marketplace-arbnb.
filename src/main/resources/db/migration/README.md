# Versioned database migrations

Store PostgreSQL migrations in this directory using Flyway versioned names.
The initial migration creates the tables assigned to Users and
Accommodations/Publications, plus their shared catalogs. The policy and service
catalogs are seeded there with fixed IDs. Do not edit an applied migration;
create a new version for later schema or catalog changes.

The approved design uses TIMESTAMP WITHOUT TIME ZONE for Instant columns, with
values normalized to UTC by the persistence converter and Hibernate JDBC
timezone setting. Availability dates use PostgreSQL DATE.
