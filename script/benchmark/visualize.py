import sys
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

feature = sys.argv[1]
data_file = sys.argv[2]
output = sys.argv[3]

target_names = {
    'record4s':     'record4s',
    'caseclass':    'case class',
    'map':          'Map',
    'shapeless':    'shapeless',
    'scalarecords': 'scala-records',
}

plot_config = {
    'Creation': {
        'xlabel': 'Record size',
        'ylabel': 'Creation time [ns]',
        'xmin': 2,
        'ymin': 0,
        'xstep': 2,
        'ystep': 500,
    },
    'Update': {
        'xlabel': 'Record size',
        'ylabel': 'Update time [ns]',
        'xmin': 20,
        'ymin': 0,
        'xstep': 20,
        'ystep': 200,
    },
    'FieldAccess': {
        'xlabel': 'Field index',
        'ylabel': 'Access time [ns]',
        'xmin': 2,
        'ymin': 0,
        'xstep': 2,
        'ystep': 5,
    },
}

conf = plot_config[feature]

df = pd.read_json(data_file)

x = 'index'
y = 'score'
xmax = df[x].max()
ymax = df[y].max()
xmin, xstep = conf['xmin'], conf['xstep']
ymin, ystep = conf['ymin'], conf['ystep']

xticks = list(range(xmin, int(xmax / xstep) * xstep + 1, xstep))
yticks = list(range(ymin, int(ymax / ystep) * ystep + 1, ystep))

sns.set_theme()
g = sns.relplot(
    data=df,
    estimator="average",
    kind="line",
    x=x,
    y=y,
    hue="target",
    style="target",
    dashes=False,
    markers=True
)
g.set_axis_labels(conf['xlabel'], conf['ylabel'])
g.ax.set_xticks(xticks)
g.ax.set_yticks(yticks)
g.legend.set(title=None)
for t in g.legend.get_texts():
    t.set_text(target_names[t.get_text()])
g.tight_layout()
g.savefig(output, format='svg')
