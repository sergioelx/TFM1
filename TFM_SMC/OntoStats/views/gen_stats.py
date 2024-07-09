from flask import render_template, Blueprint, send_file, redirect, url_for, request, session, flash
import os
import plotly.graph_objs as go
import json
import networkx as nx
from urllib.parse import urlparse
import subprocess

gen = Blueprint('gen', __name__)

@gen.route('/gen_stats', methods=('GET', 'POST'))
def gen_stats():
    purl = session.get('purl', None)

    if purl is None:
        # Manejar el caso en que no se encuentra la URL en la sesión
        return redirect(url_for('check.check_url'))

    with open('output.json') as f:
        data = json.load(f)
    
    # Crear el gráfico original
    fig = create_ontology_graph(data)
    graph_html = fig.to_html(full_html=False)

    ontology_title = data.get('ontologyTitle', None)

    metrics = ['axiomCount', 'declarationAxiomCount', 'classCount', 'objectPropertyCount', 'dataPropertyCount', 'individualCount', 'annotationPropertyCount', 'weight']
    graph_html2 = {}
    for metric in metrics:
        graph_html2[metric] = create_bar_chart(data, metric)

    # Cargar estadisticas.json y crear el gráfico de comparación si la sesión enriched está activa
    comparison_graph_html = None
    comparison_node_graph_html = None
    if session.get('enriched', False):
        with open('estadisticas.json') as f:
            stats_data = json.load(f)
        comparison_graph_html = create_comparison_chart(stats_data)
        comparison_node_graph = create_ontology_graph_with_comparison(data, stats_data)
        comparison_node_graph_html = comparison_node_graph.to_html(full_html=False)

    # Envía el archivo al cliente para descargarlo
    return render_template("gen_stats.html", graph_html=graph_html, graph_html2=graph_html2, comparison_graph_html=comparison_graph_html, comparison_node_graph_html=comparison_node_graph_html, ontology_title=ontology_title, purl=purl)


@gen.route('/download_json', methods=['GET'])
def download_json():
    # Obtener la ruta absoluta del archivo JSON
    root_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.dirname(__file__), "..", ".."))
    json_filename = 'output.json'
    json_url = os.path.join(root_dir, json_filename)

    # Enviar el archivo JSON al cliente para descargarlo
    return send_file(json_url, as_attachment=True)

def get_final_part(url):
    parsed_url = urlparse(url)
    last_part = parsed_url.path.split('/')[-1]
    return last_part.replace('.owl', '')

def get_parent_ontology(url):
    parsed_url = urlparse(url)
    parts = parsed_url.path.split('/')
    try:
        index = parts.index('imports')
        parent = parts[index - 1]
        return parent
    except ValueError:
        return None

def create_ontology_graph(data):
    G = nx.DiGraph()
    original_ontology = data['idOntology']
    original_ontology_name = get_final_part(original_ontology)
    G.add_node(original_ontology_name)

    for entry in data['imports']:
        ontology_id = entry['idOntology']
        parent_ontology = get_parent_ontology(ontology_id)
        imported_ontology_name = get_final_part(ontology_id)
        G.add_node(imported_ontology_name)
        G.add_edge(parent_ontology if parent_ontology else imported_ontology_name, imported_ontology_name)
    
    axiom_counts = {get_final_part(entry['idOntology']): entry['ontologyMetrics']['axiomCount'] for entry in data['imports']}
    axiom_counts[original_ontology_name] = data['ontologyMetrics']['axiomCount']
    
    min_axiom_count = min(axiom_counts.values())
    max_axiom_count = max(axiom_counts.values())
    scaled_sizes = {node: 20 + (axiom_counts[node] - min_axiom_count) / (max_axiom_count - min_axiom_count) * 100 for node in axiom_counts}

    pos = nx.spring_layout(G)

    node_trace = go.Scatter(x=[], y=[], text=[], mode='markers+text', hoverinfo='text', marker=dict(size=[], color='LightSkyBlue'))
    edge_trace = go.Scatter(x=[], y=[], line=dict(width=1, color='#888'))

    for node in G.nodes():
        x, y = pos[node]
        node_trace['x'] += (x,)
        node_trace['y'] += (y,)
        
        if node == original_ontology_name:
            text = node
        else:
            parent_ontology = get_parent_ontology(f"http://example.com/imports/{node}.owl")
            if parent_ontology:
                text = f"{parent_ontology}/{node}"
            else:
                text = node

        node_trace['text'] += (text,)
        node_trace['marker']['size'] += (scaled_sizes[node],)

    for edge in G.edges():
        x0, y0 = pos[edge[0]]
        x1, y1 = pos[edge[1]]
        edge_trace['x'] += (x0, x1, None)
        edge_trace['y'] += (y0, y1, None)

    fig = go.Figure(data=[edge_trace, node_trace],
                    layout=go.Layout(
                        title='Ontology Graph',
                        titlefont_size=16,
                        showlegend=False,
                        hovermode='closest',
                        margin=dict(b=20,l=5,r=5,t=40),
                        xaxis=dict(showgrid=False, zeroline=False, showticklabels=False),
                        yaxis=dict(showgrid=False, zeroline=False, showticklabels=False))
                    )
    return fig

def create_bar_chart(data, metric_name):
    ontologies = data['imports']

    ontology_names = [get_final_part(ontology['idOntology']) for ontology in ontologies]
    parent_ontology_names = [get_parent_ontology(ontology['idOntology']) for ontology in ontologies]
    metric_values = [ontology['ontologyMetrics'][metric_name] for ontology in ontologies]

    original_ontology_name = get_final_part(data['idOntology'])
    parent_ontology_name = get_parent_ontology(data['idOntology'])
    original_metric_value = data['ontologyMetrics'][metric_name]

    ontology_names.insert(0, original_ontology_name)
    parent_ontology_names.insert(0, parent_ontology_name)
    metric_values.insert(0, original_metric_value)

    colors = ['red'] + ['blue' for _ in parent_ontology_names[1:]]

    trace = go.Bar(
        x=[f"{parent}|{ontology}" if parent else ontology for ontology, parent in zip(ontology_names, parent_ontology_names)],
        y=metric_values,
        marker=dict(color=colors)
    )

    layout = go.Layout(
        title=f'Comparacion de {metric_name}',
        xaxis=dict(title='Ontologies'),
        yaxis=dict(title=metric_name)
    )

    fig = go.Figure(data=[trace], layout=layout)
    graph_html = fig.to_html(full_html=False)

    return graph_html

def create_comparison_chart(data):
    methods = ['original', 'star', 'bot', 'top']
    metrics = {
        'NumeroDeAxiomas': ['NumeroDeAxiomasOriginal', 'AxiomasFinales_star', 'AxiomasFinales_bot', 'AxiomasFinales_top'],
        'ClasesFinales': ['NumeroDeClasesOriginal', 'ClasesFinales_star', 'ClasesFinales_bot', 'ClasesFinales_top'],
        'PesoMB': ['PesoOriginalMB', 'PesoResultadoStarMB', 'PesoResultadoBotMB', 'PesoResultadoTopMB']
    }

    figures = {}

    for metric_name, metric_keys in metrics.items():
        x_values = []
        y_values = []
        for method, key in zip(methods, metric_keys):
            # Verificar si la clave está presente en el JSON
            if key in data and data[key] != 0:
                x_values.append(method)
                y_values.append(data[key])

        if x_values and y_values:
            trace = go.Bar(
                x=x_values,
                y=y_values,
                marker=dict(color='blue')
            )
            layout = go.Layout(
                title=f'Comparacion de {metric_name}',
                xaxis=dict(title='Metodos'),
                yaxis=dict(title=metric_name)
            )
            fig = go.Figure(data=[trace], layout=layout)
            figures[metric_name] = fig.to_html(full_html=False)
    
    return figures

@gen.route('/back_to_main', methods=('GET',))
def back_to_main():
    session.pop('purl', None)
    session.pop('enriched', None)
    return redirect(url_for('index'))

@gen.route('/enrich_ontology', methods=['GET'])
def enrich_ontology():
    purl = session.get('purl', None)

    ontology_url_without_extension = os.path.splitext(purl)[0]

    jar_file = "OntoStats/static/prueba6.jar"
    output_dir = "salidas/"
    java_command = f"java --add-opens java.base/java.lang=ALL-UNNAMED -Xmx10G -jar {jar_file} -t 5 {ontology_url_without_extension} {output_dir} Star"

    try:
        subprocess.run(java_command, check=True, shell=True)
        session['enriched'] = True
    except subprocess.CalledProcessError:
        flash("Error al ejecutar el archivo JAR para enriquecer la ontología.")
    
    return redirect(url_for('gen.gen_stats'))


def create_ontology_graph_with_comparison(data, stats_data):
    G = nx.DiGraph()

    # Crear el nodo de la ontología original
    original_ontology = data['idOntology']
    original_ontology_name = get_final_part(original_ontology)
    G.add_node(original_ontology_name)

    # Agregar nodos de importaciones
    for entry in data['imports']:
        ontology_id = entry['idOntology']
        parent_ontology = get_parent_ontology(ontology_id)
        imported_ontology_name = get_final_part(ontology_id)
        G.add_node(imported_ontology_name)
        G.add_edge(parent_ontology if parent_ontology else original_ontology_name, imported_ontology_name)

    # Obtener el axiom count para cada nodo
    axiom_counts = {get_final_part(entry['idOntology']): entry['ontologyMetrics']['axiomCount'] for entry in data['imports']}
    axiom_counts[original_ontology_name] = data['ontologyMetrics']['axiomCount']

    # Escalar los tamaños del círculo entre un rango definido
    min_axiom_count = min(axiom_counts.values())
    max_axiom_count = max(axiom_counts.values())
    scaled_sizes = {node: 20 + (axiom_counts[node] - min_axiom_count) / (max_axiom_count - min_axiom_count) * 100 for node in axiom_counts}

    # Añadir el tamaño del círculo adicional basado en AxiomasFinales_star
    if 'AxiomasFinales_star' in stats_data:
        axiom_count_star = stats_data['AxiomasFinales_star']
        scaled_size_star = 20 + (axiom_count_star - min_axiom_count) / (max_axiom_count - min_axiom_count) * 100
    else:
        axiom_count_star = 0
        scaled_size_star = 0

    # Calcular las posiciones de los nodos para la visualización
    pos = nx.spring_layout(G)

    # Crear los trazos del gráfico
    node_trace = go.Scatter(x=[], y=[], text=[], mode='markers+text', hoverinfo='text', 
                            marker=dict(size=[], color=[], opacity=0.5, colorscale='Viridis'))
    edge_trace = go.Scatter(x=[], y=[], line=dict(width=1, color='#888'))

    # Agregar posiciones de los nodos al trazo de los nodos
    for node in G.nodes():
        x, y = pos[node]
        node_trace['x'] += (x,)
        node_trace['y'] += (y,)

        # Crear el texto que se mostrará al pasar el ratón sobre el nodo
        if node == original_ontology_name:
            text = node
        else:
            parent_ontology = get_parent_ontology(f"http://example.com/imports/{node}.owl")
            if parent_ontology:
                text = f"{parent_ontology}/{node}"
            else:
                text = node

        node_trace['text'] += (text,)
        node_trace['marker']['size'] += (scaled_sizes[node],)
        node_trace['marker']['color'] += ('LightSkyBlue',)

    # Crear un trazo separado para el nodo adicional con el tamaño de AxiomasFinales_star
    comparison_trace = go.Scatter(
        x=[pos[original_ontology_name][0]], y=[pos[original_ontology_name][1]],
        mode='markers', hoverinfo='none',
        marker=dict(size=[scaled_size_star], color=['red'], opacity=0.5)
    )

    # Agregar posiciones de los bordes al trazo de los bordes
    for edge in G.edges():
        x0, y0 = pos[edge[0]]
        x1, y1 = pos[edge[1]]
        edge_trace['x'] += (x0, x1, None)
        edge_trace['y'] += (y0, y1, None)

    # Crear el gráfico
    fig = go.Figure(data=[edge_trace, comparison_trace, node_trace],
                    layout=go.Layout(
                        title='Ontology Graph with AxiomasFinales_star Comparison',
                        titlefont_size=16,
                        showlegend=False,
                        hovermode='closest',
                        margin=dict(b=20, l=5, r=5, t=40),
                        xaxis=dict(showgrid=False, zeroline=False, showticklabels=False),
                        yaxis=dict(showgrid=False, zeroline=False, showticklabels=False))
                    )
    return fig



