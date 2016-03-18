#!/usr/bin/python

import subprocess
import sys
import random
import signal
import operator
from math import floor
from multiprocessing import Pool

# have to keep the rungame process in a method outside the class due to how multiprocessing pool works
def runGame(weights):
    signal.signal(signal.SIGINT, signal.SIG_IGN)

    command = ["java", "PlayerSkeleton"] + [str(x) for x in weights]
    process = subprocess.Popen(command, stdout=subprocess.PIPE)
    numRowsCleared, numTurns = process.stdout.read().decode("utf-8").strip("\r\n").split(" ")
    return (int(numRowsCleared), numTurns)

class AlphaTetris():
    """Genetic Algorithm to optimise weights for heuristics in PlayerSkeleton.java"""

    pool_size = 4
    defaultWeights = [
        -0.510066, # Aggregate column heights
        -0.184483, # Bumpiness
        0, # Max height
        -0.6, # Num of holes created
        0.760666 # Num of completed rows
    ]

    def __init__(self):
        self.pool = Pool(AlphaTetris.pool_size)
        self.populationCount = 32    # Must be even
        self.mutationRate = 0.05    # [0, 1]
        self.generation = 1
        self.start([AlphaTetris.defaultWeights] + [self.generateWeights(AlphaTetris.pool_size) for i in range(self.populationCount - 1)])

    def start(self, populationWeights):
        print("Generation", self.generation)

        choices = []
        pool = Pool(self.pool_size)

        try:
            scores = pool.map(runGame, populationWeights)
        except KeyboardInterrupt as ki:
            pool.join()
            pool.terminate()
        else:
            for (numRowsCleared, numTurns), weights in zip(scores, populationWeights):
                print("Weights used:", weights)
                print("Rows cleared:", numRowsCleared, "Number of turns:", numTurns)
                choices.append((weights, numRowsCleared))

            parents = self.selectParents(choices)
            self.crossover(parents)
            self.mutate(parents)

            self.generation += 1
            self.start(parents)

    #### Initial population
    def generateWeights(self, seed):
        return [self.generateWeight(i) for i in AlphaTetris.defaultWeights]

    def generateWeight(self, origin):
        return origin + random.uniform(-0.1, 0.1)



    #### Selection
    def selectParents(self, choices):
        return [self.selectParent(choices) for i in range(self.populationCount)]

    def selectParent(self, choices):
        total = sum(score for weights, score in choices)
        randomVar = random.uniform(0, total)
        currentSum = 0

        for weights, score in choices:
            if currentSum + score >= randomVar:
                return weights
            currentSum += score

        assert False, "Shouldn't get here"

    #### Crossover
    def crossover(self, parents):
        for i in range(0, self.populationCount, 2):
            crosspoint = random.randint(0, len(AlphaTetris.defaultWeights) - 1)
            temp = parents[i][:crosspoint]
            parents[i][:crosspoint] = parents[i+1][:crosspoint]
            parents[i+1][:crosspoint] = temp


    #### Mutation
    def mutate(self, parents):
        for parent in parents:
            if random.uniform(0, 1) < self.mutationRate:
                mutation_target = random.randint(0, len(AlphaTetris.defaultWeights) - 1)
                parent[mutation_target] = self.generateWeight(parent[mutation_target])



if __name__ == '__main__':
	AlphaTetris()