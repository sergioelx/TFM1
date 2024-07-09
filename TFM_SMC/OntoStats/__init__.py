from flask import Flask, render_template, session

app = Flask(__name__)


app.config.from_object('config.DevelopmentConfig')
app.secret_key = 'dev'

# #Importar vistas 
from OntoStats.views.gen_stats import gen
app.register_blueprint(gen)
from OntoStats.views.check_url import check
app.register_blueprint(check)


@app.route('/')
def index():
    session.pop('purl', None)
    session.pop('enriched', None)
    return render_template('principal.html')

#Configuración de las páginas de Enlaces rápidos
@app.route('/quienes_somos')
def quienes_somos():
    return render_template('enlaces_rapidos/quienes_somos.html')

@app.route('/politica_privacidad')
def politica_privacidad():
    return render_template('enlaces_rapidos/politica_privacidad.html')

@app.route('/terminos_condiciones')
def terminos_condiciones():
    return render_template('enlaces_rapidos/terminos_condiciones.html')

@app.route('/contacto')
def contacto():
    return render_template('enlaces_rapidos/contacto.html')