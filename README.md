# Lab2_BDA
Laboratorio N°2 de Base de datos avanzada

Para clonar el repositorio en su computador, abra una consola ya sea cmd o algun powershell en la ruta de destino e ingrese el comando: git clone https://github.com/IgnacioCelis2003/Lab2_BDA.git

Una vez clonado el repositorio abra el programa que tiene para ver la base de datos (ya sea pgAdmin 4 u otros), una vez dentro del programa, en el servidor PostgreSQL 17 (instalar PostgreSQL 17 en caso de no tenerlo) se debe crear una base de datos llamada Lab2BDA. Asegurarse también tener instalada la extensión de PostGIS, versión 3.6.1.

Con la base de datos creada, se debe abrir una ventana de consulta Query dentro de la base de datos Lab2BDA, una vez dentro de la pestaña se debe abrir el archivo llamado Crear_BD.sql copiar todo su contenido, pegarlo en la pestaña de consuta Query y ejecutarlo. Si da algun error con respecto a malformacion de tablas o conflictos entre tablas, agregar y ejecutar uno por uno los CREATE.

Ya creadas las tablas, abrir el archivo Datos_prueba.sql copiar los INSERT INTO *tabla*(*columnas*) VALUES(*valores*) uno por uno en el orden en el que se encuentran en el archivo, copiar y ejecutar de a más lleva a errores, pegarlos en la misma pestaña Query y ejecutarlo.

Luego de rellenar las tablas con los datos se puede ejecutar el programa. Abrir intellij IDEA (o su IDE de preferencia), buscar la carpeta del proyecto, buscar la carpeta Backend y abrirla. Una vez abierta se busca el archivo BackendApplication, se abre y se ejecuta (revisar que a la izquierda del boton de ejecutar diga "current file" o "BackendApplication"). Para ejecutar el frontend se abre Visual Studio Code, se busca la carpeta del proyecto, buscar la carpeta Frontend_Nuxt y abrirla. Para ejecutar el frontend se debe abrir una terminal en Visual Studio Code y ejecutar el comando npm i (isntalará todas las dependencias necesarias), una vez instaladas todas las dependencias, ejecutar el comando npm run dev, esperar a que cargue y hacer crtl + click en http://localhost:3000/ esto llevará a la página.

Nota: El frontend hace uso de la API de elevación de ArcGIS Elevation Services para determinar la altitud de un punto, dada la longitud y latitud.
En caso que la conexión a la API falle, o no se tiene conexión a internet, es posible establecer valores de altitud de forma manual.
