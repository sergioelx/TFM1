from flask import (
    render_template, Blueprint, flash, redirect, request, url_for, session
)
import re  # Importar el módulo de expresiones regulares
import subprocess

check = Blueprint('check', __name__)

@check.route('/', methods=('GET', 'POST'))
def check_url():
    if request.method == 'POST':
        # Obtener la URL del formulario de registro
        purl = request.form.get('purl')


        # Expresión regular para validar la URL
        url_regex = r'^https?://(?:www\.)?\S+$'

        # Validar la URL introducida
        if not re.match(url_regex, purl):
            flash("La URL introducida no tiene el formato correcto.")
            return redirect(url_for('check.check_stats'))

        # Ruta al archivo JAR
        jar_file = "OntoStats/static/prueba4.jar"

        # Comando para ejecutar el archivo JAR con la URL como argumento
        java_command1 = f"java -jar {jar_file} {purl}"

        try:
            # Ejecutar el archivo JAR
            subprocess.run(java_command1, check=True)
        except subprocess.CalledProcessError as e:
            # Manejar errores si la ejecución del archivo JAR falla
            flash("Error al ejecutar el archivo JAR.")
            return redirect(url_for('check.check_stats'))

        # Guardar la URL en la sesión
        session['purl'] = purl

        # Si la ejecución es exitosa, redireccionar o mostrar un mensaje de éxito
        flash("Archivo JAR ejecutado exitosamente con la URL proporcionada.")
        return redirect(url_for('gen.gen_stats'))

    return render_template('principal.html')
