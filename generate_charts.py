"""
Chart Generator for Hybrid GWO-SA Cloud Task Scheduling Project
Generates comprehensive visual benchmark charts comparing Hybrid GWO-SA
against 6 classical/pure baselines and 3 state-of-the-art 2025 hybrid algorithms.
"""
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib
import numpy as np
import os

matplotlib.use('Agg')  # Headless backend

RESULTS_DIR = r"D:\CS FUNDAMENTALS\OOPS\Projects\Hybrid_GWO_SA\results"
CHARTS_DIR = os.path.join(RESULTS_DIR, "charts")
os.makedirs(CHARTS_DIR, exist_ok=True)

# Load data
df = pd.read_csv(os.path.join(RESULTS_DIR, "comparison_results.csv"))

# Color scheme distinguishing heuristics, pure metaheuristics, 2025 hybrids, and proposed GWO-SA
COLORS = {
    'Round Robin': '#95a5a6',                 # Gray
    'FCFS': '#7f8c8d',                        # Dark Gray
    'Min-Min': '#34495e',                     # Slate
    'Pure GWO': '#e67e22',                    # Orange
    'Pure SA': '#f1c40f',                     # Yellow
    'Pure PSO': '#9b59b6',                    # Purple
    'Hybrid WOA-GS [Acharya \'25]': '#3498db', # Blue
    'Hybrid PSO-GWO [Kumari \'25]': '#1abc9c', # Teal
    'Hybrid DECSO [Gunduz \'25]': '#e74c3c',   # Red
    'Hybrid GWO-SA': '#27ae60'                # Emerald Green (Proposed)
}

ALGO_ORDER = [
    'Round Robin',
    'FCFS',
    'Min-Min',
    'Pure GWO',
    'Pure SA',
    'Pure PSO',
    'Hybrid WOA-GS [Acharya \'25]',
    'Hybrid PSO-GWO [Kumari \'25]',
    'Hybrid DECSO [Gunduz \'25]',
    'Hybrid GWO-SA'
]

ALGO_SHORT = [
    'RR', 'FCFS', 'MM', 'GWO', 'SA', 'PSO',
    'WOA-GS\n(\'25)', 'PSO-GWO\n(\'25)', 'DECSO\n(\'25)',
    'GWO-SA\n(Ours)'
]

SCENARIOS = ['Small', 'Medium', 'Large', 'XLarge']

# =========================================================================
# CHART 1: Makespan Comparison Bar Chart (All 10 Algorithms across 4 Scales)
# =========================================================================
fig, axes = plt.subplots(1, 4, figsize=(22, 6), sharey=False)
fig.suptitle('Makespan Comparison Across Cloud Scenarios (Lower is Better)', fontsize=16, fontweight='bold', y=0.98)

for idx, scenario in enumerate(SCENARIOS):
    ax = axes[idx]
    data = df[df['Scenario'] == scenario].set_index('Algorithm').loc[ALGO_ORDER]
    bars = ax.bar(range(len(ALGO_ORDER)), data['Makespan'],
                  color=[COLORS[a] for a in ALGO_ORDER], edgecolor='black', linewidth=0.6)
    ax.set_title(f'{scenario} Scenario', fontsize=13, fontweight='bold')
    ax.set_xticks(range(len(ALGO_ORDER)))
    ax.set_xticklabels(ALGO_SHORT, rotation=35, ha='right', fontsize=9)
    ax.set_ylabel('Makespan (seconds)' if idx == 0 else '', fontsize=11)
    ax.grid(axis='y', alpha=0.3, linestyle='--')

    # Highlight best performer
    best_idx = data['Makespan'].values.argmin()
    bars[best_idx].set_edgecolor('#d35400')
    bars[best_idx].set_linewidth(2.5)

plt.tight_layout()
plt.savefig(os.path.join(CHARTS_DIR, 'makespan_comparison.png'), dpi=200, bbox_inches='tight')
plt.close()
print("[OK] makespan_comparison.png")

# =========================================================================
# CHART 2: Degree of Imbalance (DoI) Comparison (Workload Balance)
# =========================================================================
fig, axes = plt.subplots(1, 4, figsize=(22, 6), sharey=False)
fig.suptitle('Degree of Imbalance (DoI) Across Cloud Scenarios (Lower is Better)', fontsize=16, fontweight='bold', y=0.98)

for idx, scenario in enumerate(SCENARIOS):
    ax = axes[idx]
    data = df[df['Scenario'] == scenario].set_index('Algorithm').loc[ALGO_ORDER]
    bars = ax.bar(range(len(ALGO_ORDER)), data['DoI'],
                  color=[COLORS[a] for a in ALGO_ORDER], edgecolor='black', linewidth=0.6)
    ax.set_title(f'{scenario} Scenario', fontsize=13, fontweight='bold')
    ax.set_xticks(range(len(ALGO_ORDER)))
    ax.set_xticklabels(ALGO_SHORT, rotation=35, ha='right', fontsize=9)
    ax.set_ylabel('Degree of Imbalance' if idx == 0 else '', fontsize=11)
    ax.grid(axis='y', alpha=0.3, linestyle='--')

    best_idx = data['DoI'].values.argmin()
    bars[best_idx].set_edgecolor('#d35400')
    bars[best_idx].set_linewidth(2.5)

plt.tight_layout()
plt.savefig(os.path.join(CHARTS_DIR, 'doi_comparison.png'), dpi=200, bbox_inches='tight')
plt.close()
print("[OK] doi_comparison.png")

# =========================================================================
# CHART 3: Execution Cost Comparison
# =========================================================================
fig, axes = plt.subplots(1, 4, figsize=(22, 6), sharey=False)
fig.suptitle('Execution Cost Comparison Across Scenarios (Balanced Trade-Off)', fontsize=16, fontweight='bold', y=0.98)

for idx, scenario in enumerate(SCENARIOS):
    ax = axes[idx]
    data = df[df['Scenario'] == scenario].set_index('Algorithm').loc[ALGO_ORDER]
    bars = ax.bar(range(len(ALGO_ORDER)), data['Cost'],
                  color=[COLORS[a] for a in ALGO_ORDER], edgecolor='black', linewidth=0.6)
    ax.set_title(f'{scenario} Scenario', fontsize=13, fontweight='bold')
    ax.set_xticks(range(len(ALGO_ORDER)))
    ax.set_xticklabels(ALGO_SHORT, rotation=35, ha='right', fontsize=9)
    ax.set_ylabel('Execution Cost ($)' if idx == 0 else '', fontsize=11)
    ax.grid(axis='y', alpha=0.3, linestyle='--')

plt.tight_layout()
plt.savefig(os.path.join(CHARTS_DIR, 'cost_comparison.png'), dpi=200, bbox_inches='tight')
plt.close()
print("[OK] cost_comparison.png")

# =========================================================================
# CHART 4: Convergence Curve (Iteration vs. Fitness)
# =========================================================================
conv_file = os.path.join(RESULTS_DIR, "convergence.csv")
if os.path.exists(conv_file):
    conv_df = pd.read_csv(conv_file)
    fig, ax = plt.subplots(figsize=(11, 6))

    for col in conv_df.columns:
        if col == 'Iteration':
            continue
        c = COLORS.get(col, '#555555')
        is_ours = ('GWO-SA' in col and 'PSO' not in col)
        lw = 3.5 if is_ours else 1.8
        alpha = 1.0 if is_ours else 0.75
        ax.plot(conv_df['Iteration'], conv_df[col], label=col, color=c, linewidth=lw, alpha=alpha)

    ax.set_xlabel('Iteration Number', fontsize=12, fontweight='bold')
    ax.set_ylabel('Multi-Objective Fitness (Lower is Better)', fontsize=12, fontweight='bold')
    ax.set_title('Convergence Progression — Small Scenario (100 Tasks, 10 VMs)', fontsize=14, fontweight='bold')
    ax.legend(fontsize=9, loc='upper right', framealpha=0.9)
    ax.grid(True, alpha=0.3, linestyle='--')
    plt.tight_layout()
    plt.savefig(os.path.join(CHARTS_DIR, 'convergence_curve.png'), dpi=200, bbox_inches='tight')
    plt.close()
    print("[OK] convergence_curve.png")

# =========================================================================
# CHART 5: Improvement Heatmap (% Improvement Over All 9 Baselines)
# =========================================================================
hybrid_label = 'Hybrid GWO-SA'
baselines = [a for a in ALGO_ORDER if a != hybrid_label]

imp_data = []
for scenario in SCENARIOS:
    row = []
    sc_data = df[df['Scenario'] == scenario].set_index('Algorithm')
    hybrid_vals = sc_data.loc[hybrid_label]
    for baseline in baselines:
        base_vals = sc_data.loc[baseline]
        mk_imp = (base_vals['Makespan'] - hybrid_vals['Makespan']) / base_vals['Makespan'] * 100.0
        row.append(mk_imp)
    imp_data.append(row)

imp_matrix = np.array(imp_data)

fig, ax = plt.subplots(figsize=(13, 6))
im = ax.imshow(imp_matrix, cmap='RdYlGn', aspect='auto', vmin=-10, vmax=70)
ax.set_xticks(range(len(baselines)))
ax.set_xticklabels(baselines, rotation=30, ha='right', fontsize=10, fontweight='bold')
ax.set_yticks(range(len(SCENARIOS)))
ax.set_yticklabels(SCENARIOS, fontsize=11, fontweight='bold')
ax.set_title('Makespan Improvement (%) of Hybrid GWO-SA Over All Baselines', fontsize=15, fontweight='bold', pad=15)

# Text labels on matrix
for i in range(len(SCENARIOS)):
    for j in range(len(baselines)):
        val = imp_matrix[i, j]
        color = 'white' if (val > 40 or val < 0) else 'black'
        ax.text(j, i, f'{val:+.1f}%', ha='center', va='center', fontsize=10, fontweight='bold', color=color)

cbar = plt.colorbar(im, ax=ax, fraction=0.03, pad=0.03)
cbar.set_label('Makespan Improvement %', fontsize=11, fontweight='bold')
plt.tight_layout()
plt.savefig(os.path.join(CHARTS_DIR, 'improvement_heatmap.png'), dpi=200, bbox_inches='tight')
plt.close()
print("[OK] improvement_heatmap.png")

# =========================================================================
# CHART 6: Radar Chart — Multi-Objective Trade-Off (Medium Scenario)
# =========================================================================
fig, ax = plt.subplots(figsize=(8, 8), subplot_kw=dict(polar=True))
scenario = 'Medium'
sc_data = df[df['Scenario'] == scenario].set_index('Algorithm')

categories = ['Speed (1/Makespan)', 'Cost Efficiency (1/Cost)', 'Fair Balance (1/DoI)']
N = len(categories)
angles = np.linspace(0, 2 * np.pi, N, endpoint=False).tolist()
angles += angles[:1]

selected_algos = [
    'Min-Min',
    'Pure GWO',
    'Hybrid WOA-GS [Acharya \'25]',
    'Hybrid PSO-GWO [Kumari \'25]',
    'Hybrid DECSO [Gunduz \'25]',
    'Hybrid GWO-SA'
]

for algo in selected_algos:
    vals = []
    for metric in ['Makespan', 'Cost', 'DoI']:
        all_vals = sc_data[metric]
        norm = (sc_data.loc[algo, metric] - all_vals.min()) / (all_vals.max() - all_vals.min()) if all_vals.max() != all_vals.min() else 0
        vals.append(1.0 - norm)  # Invert so higher = better
    vals += vals[:1]
    color = COLORS.get(algo, '#333333')
    is_ours = (algo == 'Hybrid GWO-SA')
    lw = 3.5 if is_ours else 1.8
    alpha_fill = 0.25 if is_ours else 0.08
    ax.plot(angles, vals, 'o-', linewidth=lw, label=algo, color=color)
    ax.fill(angles, vals, alpha=alpha_fill, color=color)

ax.set_xticks(angles[:-1])
ax.set_xticklabels(categories, fontsize=11, fontweight='bold')
ax.set_title('Multi-Objective Optimization Trade-Off (Medium Scenario)\n(Larger Polygon Area = Superior Overall Performance)',
             fontsize=13, fontweight='bold', pad=25)
ax.legend(loc='upper right', bbox_to_anchor=(1.35, 1.1), fontsize=9)
plt.tight_layout()
plt.savefig(os.path.join(CHARTS_DIR, 'radar_chart.png'), dpi=200, bbox_inches='tight')
plt.close()
print("[OK] radar_chart.png")

# =========================================================================
# CHART 7: Head-to-Head Hybrid Comparison (Hybrid GWO-SA vs. 3 2025 Papers)
# =========================================================================
hybrid_sub = [
    'Hybrid WOA-GS [Acharya \'25]',
    'Hybrid PSO-GWO [Kumari \'25]',
    'Hybrid DECSO [Gunduz \'25]',
    'Hybrid GWO-SA'
]
hybrid_names = ['WOA-GS (2025)', 'PSO-GWO (2025)', 'DECSO (2025)', 'Hybrid GWO-SA (Proposed)']

fig, axes = plt.subplots(1, 2, figsize=(16, 6))

# Subplot 1: Large Scenario Makespan
large_data = df[df['Scenario'] == 'Large'].set_index('Algorithm').loc[hybrid_sub]
axes[0].bar(range(len(hybrid_sub)), large_data['Makespan'],
            color=[COLORS[h] for h in hybrid_sub], edgecolor='black', linewidth=0.8)
axes[0].set_title('Large Scenario (500 Tasks) Makespan', fontsize=13, fontweight='bold')
axes[0].set_xticks(range(len(hybrid_sub)))
axes[0].set_xticklabels(hybrid_names, rotation=20, ha='right', fontsize=10, fontweight='bold')
axes[0].set_ylabel('Makespan (seconds)', fontsize=11)
axes[0].grid(axis='y', alpha=0.3)

# Subplot 2: XLarge Scenario Makespan
xlarge_data = df[df['Scenario'] == 'XLarge'].set_index('Algorithm').loc[hybrid_sub]
axes[1].bar(range(len(hybrid_sub)), xlarge_data['Makespan'],
            color=[COLORS[h] for h in hybrid_sub], edgecolor='black', linewidth=0.8)
axes[1].set_title('XLarge Scenario (1000 Tasks) Makespan', fontsize=13, fontweight='bold')
axes[1].set_xticks(range(len(hybrid_sub)))
axes[1].set_xticklabels(hybrid_names, rotation=20, ha='right', fontsize=10, fontweight='bold')
axes[1].set_ylabel('Makespan (seconds)', fontsize=11)
axes[1].grid(axis='y', alpha=0.3)

fig.suptitle('Head-to-Head Comparison: Proposed Hybrid GWO-SA vs. 2025 State-of-the-Art Papers',
             fontsize=15, fontweight='bold')
plt.tight_layout()
plt.savefig(os.path.join(CHARTS_DIR, 'hybrid_vs_hybrid_comparison.png'), dpi=200, bbox_inches='tight')
plt.close()
print("[OK] hybrid_vs_hybrid_comparison.png")

# =========================================================================
# CHART 8: Computational Execution Time (ms)
# =========================================================================
fig, ax = plt.subplots(figsize=(14, 6))
x = np.arange(len(SCENARIOS))
width = 0.085

for idx, algo in enumerate(ALGO_ORDER):
    times = [df[(df['Scenario'] == s) & (df['Algorithm'] == algo)]['ExecutionTimeMs'].values[0] for s in SCENARIOS]
    ax.bar(x + idx * width, times, width, label=algo, color=COLORS[algo], edgecolor='black', linewidth=0.5)

ax.set_xlabel('Workload Scenario', fontsize=12, fontweight='bold')
ax.set_ylabel('Execution Time (milliseconds, log scale)', fontsize=12, fontweight='bold')
ax.set_yscale('log')
ax.set_title('Algorithm Execution Time Comparison (Logarithmic Scale)', fontsize=14, fontweight='bold')
ax.set_xticks(x + width * 4.5)
ax.set_xticklabels(SCENARIOS, fontsize=11, fontweight='bold')
ax.legend(fontsize=8.5, ncol=2, loc='upper left')
ax.grid(True, alpha=0.3, axis='y', which='both')
plt.tight_layout()
plt.savefig(os.path.join(CHARTS_DIR, 'execution_time.png'), dpi=200, bbox_inches='tight')
plt.close()
print("[OK] execution_time.png")

print(f"\n[DONE] All 8 high-resolution charts saved to: {CHARTS_DIR}")
