import sys
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

feature = sys.argv[1]
data_file = sys.argv[2]
output = sys.argv[3]

target_names = {
    'record4s':             'record4s %',
    'record4s_arrayrecord': 'record4s ArrayRecord',
    'caseclass':            'Scala 3 case class',
    'map':                  'Scala 3 Map',
    'shapeless':            'shapeless Record',
    'scalarecords':         'scala-records Rec',
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
        'xmin': 0,
        'ymin': 0,
        'xstep': 50,
        'ystep': 500,
    },
    'Concatenation': {
        'xlabel': 'Record size',
        'ylabel': 'Concatenation time [ns]',
        'xmin': 20,
        'ymin': 0,
        'xstep': 20,
        'ystep': 2000,
    },
    'FieldAccess': {
        'xlabel': 'Field index',
        'ylabel': 'Access time [ns]',
        'xmin': 2,
        'ymin': 0,
        'xstep': 2,
        'ystep': 5,
    },
    'FieldAccessSize': {
        'xlabel': 'Record size',
        'ylabel': 'Access time [ns]',
        'xmin': 2,
        'ymin': 0,
        'xstep': 2,
        'ystep': 5,
    },
    'FieldAccessPoly': {
        'xlabel': 'Degree of polymorphism',
        'ylabel': 'Access time [ns]',
        'xmin': 2,
        'ymin': 0,
        'xstep': 2,
        'ystep': 5,
    },
    'CompileCreation': {
        'xlabel': 'Record size',
        'ylabel': 'Compilation time (record creation) [s]',
        'xmin': 0,
        'ymin': 0,
        'xstep': 50,
        'ystep': 2,
        'estimator': 'mean',
    },
    'CompileCreationAndAccess': {
        'xlabel': 'Record size',
        'ylabel': "Compilation time [s]\n(record creation and all field access)",
        'xmin': 0,
        'ymin': 0,
        'xstep': 50,
        'ystep': 5,
        'estimator': 'mean',
    },
    'CompileCreationAndAccessRep': {
        'xlabel': 'Record size',
        'ylabel': "Compilation time [s]\n(record creation and repeated field access)",
        'xmin': 0,
        'ymin': 0,
        'xstep': 50,
        'ystep': 2,
        'estimator': 'mean',
    },
    'CompileUpdate': {
        'xlabel': 'Record size',
        'ylabel': 'Compilation time (field update) [s]',
        'xmin': 0,
        'ymin': 0,
        'xstep': 50,
        'ystep': 1,
        'estimator': 'mean',
    },
    'CompileUpdateRep': {
        'xlabel': 'Repetitions',
        'ylabel': 'Compilation time (repeted field updates) [s]',
        'xmin': 0,
        'ymin': 0,
        'xstep': 5,
        'ystep': 1,
        'estimator': 'mean',
    },
    'CompileConcatenation': {
        'xlabel': 'Record size',
        'ylabel': 'Compilation time (concatenation) [s]',
        'xmin': 0,
        'ymin': 0,
        'xstep': 50,
        'ystep': 2,
        'estimator': 'mean',
    },
    'CompileFieldAccess': {
        'xlabel': 'Field index',
        'ylabel': 'Compilation time (field access) [s]',
        'xmin': 2,
        'ymin': 0,
        'xstep': 2,
        'ystep': 0.5,
    },
    'CompileFieldAccessSize': {
        'xlabel': 'Record size',
        'ylabel': 'Compilation time (field access) [s]',
        'xmin': 2,
        'ymin': 0,
        'xstep': 2,
        'ystep': 0.5,
    },
}

def ticks(min, max, step):
    min = int(min * 10)
    max = int(max * 10)
    step = int(step * 10)
    return list(map(lambda n: n / 10, range(min, int(max / step) * step + 1, step)))

conf = plot_config[feature]

df = pd.read_json(data_file)
df['target'] = df['target'].map(target_names)

x = 'index'
y = 'score'
xmax = df[x].max()
ymax = df[y].max()
xmin, xstep = conf['xmin'], conf['xstep']
ymin, ystep = conf['ymin'], conf['ystep']

xticks = ticks(xmin, xmax, xstep)
yticks = ticks(ymin, ymax, ystep)

sns.set_theme()
g = sns.relplot(
    data=df,
    estimator=conf.get('estimator', "average"),
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
g.tight_layout()
g.savefig(output, format='svg')
