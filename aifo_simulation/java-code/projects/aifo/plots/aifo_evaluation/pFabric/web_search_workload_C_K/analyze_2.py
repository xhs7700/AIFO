from os import getcwd

lams = [3600, 5200, 7000, 8900, 11100, 14150, 19000]
k_list = ['0.4', '0.55', '0.75', '1.0', '1.4', '2.0', '3.0']


def computeFCT(key):
    l = [[0 for k in k_list] for lam in lams]
    for lam_i, lam in enumerate(lams):
        for k_i, k in enumerate(k_list):
            file_path = f'temp/aifo/aifo_evaluation/pFabric/web_search_workload_C_K/{lam}/EAIFO_K{k}/analysis/flow_completion.statistics'
            with open(file_path, 'r') as f:
                lines = f.readlines()
                for line in lines:
                    if key in line:
                        val = float(line.split('=')[1].strip())
                        l[lam_i][k_i] = f'{val:.6f}'
                        break
    output_path = f'projects/aifo/plots/aifo_evaluation/pFabric/web_search_workload_C_K/{key}.dat'
    with open(output_path, 'w') as f:
        header = '\t'.join(map(lambda s: f'EAIFO_K{s}', k_list))
        f.write(f'#\t{header}\n')
        for lam_i, lam in enumerate(lams):
            content = '\t'.join(l[lam_i])
            f.write(f'{lam}\t{content}\n')


if __name__ == '__main__':
    computeFCT("less_100KB_99th_fct_ms")
    computeFCT("less_100KB_mean_fct_ms")
    computeFCT("geq_1MB_mean_fct_ms")
    computeFCT("geq_10MB_mean_fct_ms")
    pass
