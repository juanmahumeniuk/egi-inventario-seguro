// =============================================================================
// Migración V1 (MongoDB): colección "maquina"
// Almacena los atributos de hardware de cada máquina.
// El campo _id se corresponde con el id de la máquina en SQL (mismo valor).
// Ejecutar con: mongosh "<connection-string>" migraciones/mongodb/V1__create_maquina_collection.js
// =============================================================================

db.createCollection("maquina", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      title: "Validación de atributos de hardware de la máquina",
      required: [
        "_id",
        "fabricante",
        "modelo",
        "cpu",
        "ram",
        "disco",
        "tipo",
        "os",
      ],
      properties: {
        _id: {
          bsonType: ["long", "int"],
          description: "Id compartido con la entidad Maquina en SQL. Obligatorio.",
        },
        fabricante: {
          bsonType: "string",
          description: "Fabricante del equipo. Obligatorio.",
        },
        modelo: {
          bsonType: "string",
          description: "Modelo del equipo. Obligatorio.",
        },
        cpu: {
          bsonType: "string",
          description: "Procesador. Obligatorio.",
        },
        ram: {
          bsonType: "string",
          description: "Memoria RAM (ej. '16 GB'). Obligatorio.",
        },
        disco: {
          bsonType: "string",
          description: "Almacenamiento (ej. '512 GB SSD'). Obligatorio.",
        },
        tipo: {
          enum: ["DESKTOP", "LAPTOP"],
          description: "Tipo de equipo. Debe ser DESKTOP o LAPTOP. Obligatorio.",
        },
        os: {
          bsonType: "string",
          description: "Sistema operativo. Obligatorio.",
        },
        monitor: {
          bsonType: "string",
          description: "Monitor. Opcional (laptops pueden no tener uno externo).",
        },
        mouse: {
          bsonType: "string",
          description: "Mouse. Opcional.",
        },
        teclado: {
          bsonType: "string",
          description: "Teclado. Opcional.",
        },
      },
      additionalProperties: false,
    },
  },
  validationLevel: "strict",
  validationAction: "error",
});
