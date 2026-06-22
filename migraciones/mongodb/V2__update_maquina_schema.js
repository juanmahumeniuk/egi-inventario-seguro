// =============================================================================
// Migración V2 (MongoDB): actualiza el validador de la colección "maquina"
// al esquema vigente tras el rediseño del modelo (objetos anidados).
//
// Cambios respecto de V1:
//   - ram (String)  -> ramGb (Int)
//   - disco (String) -> disco { tipo, capacidadGb } (objeto embebido)
//   - os (String)    -> sistemaOperativo (String)
//   - monitor/mouse/teclado sueltos -> perifericos { monitor, mouse, teclado }
//   - tipo admite ALL_IN_ONE además de DESKTOP y LAPTOP
//   - se admite el campo _class que agrega Spring Data MongoDB
//
// Los nombres de campos son camelCase porque así los persiste Spring Data
// (la API REST expone snake_case, pero eso es solo la capa Jackson).
//
// Ejecutar con: mongosh "<connection-string>" migraciones/mongodb/V2__update_maquina_schema.js
// =============================================================================

const validator = {
  $jsonSchema: {
    bsonType: "object",
    title: "Validación de atributos de hardware de la máquina (v2)",
    required: [
      "_id",
      "fabricante",
      "modelo",
      "tipo",
      "cpu",
      "ramGb",
      "disco",
      "sistemaOperativo",
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
      tipo: {
        enum: ["DESKTOP", "LAPTOP", "ALL_IN_ONE"],
        description: "Tipo de equipo. Obligatorio.",
      },
      cpu: {
        bsonType: "string",
        description: "Procesador. Obligatorio.",
      },
      ramGb: {
        bsonType: ["int", "long"],
        description: "Memoria RAM en GB. Obligatorio.",
      },
      disco: {
        bsonType: "object",
        required: ["tipo", "capacidadGb"],
        properties: {
          tipo: {
            enum: ["SSD", "HDD"],
            description: "Tecnología del disco.",
          },
          capacidadGb: {
            bsonType: ["int", "long"],
            description: "Capacidad en GB.",
          },
        },
        additionalProperties: false,
        description: "Almacenamiento del equipo. Obligatorio.",
      },
      sistemaOperativo: {
        bsonType: "string",
        description: "Sistema operativo instalado. Obligatorio.",
      },
      perifericos: {
        bsonType: "object",
        properties: {
          monitor: { bsonType: "string" },
          mouse: { bsonType: "string" },
          teclado: { bsonType: "string" },
        },
        additionalProperties: false,
        description: "Periféricos asociados. Opcional (ej. laptops).",
      },
      _class: {
        bsonType: "string",
        description: "Discriminador de tipo agregado por Spring Data MongoDB.",
      },
    },
    additionalProperties: false,
  },
};

if (db.getCollectionNames().includes("maquina")) {
  // La colección existe (creada por V1 o implícitamente por la aplicación):
  // se reemplaza el validador con collMod.
  db.runCommand({
    collMod: "maquina",
    validator: validator,
    validationLevel: "strict",
    validationAction: "error",
  });
  print("V2: validador de 'maquina' actualizado con collMod.");
} else {
  db.createCollection("maquina", {
    validator: validator,
    validationLevel: "strict",
    validationAction: "error",
  });
  print("V2: colección 'maquina' creada con el validador v2.");
}
