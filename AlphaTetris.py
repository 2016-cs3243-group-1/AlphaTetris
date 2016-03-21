#!/usr/bin/python

import subprocess
import sys
import random
import signal
import logging
import time
from math import floor
from multiprocessing import Pool

numAttempts = 5

# have to keep the rungame process in a method outside the class due to how multiprocessing pool works
def runGame(weights):
    signal.signal(signal.SIGINT, signal.SIG_IGN)

    command = ["java", "PlayerSkeleton"] + [str(x) for x in weights]
    totalRowsCleared = 0
    totalTurns = 0
    for attempt in range(numAttempts):
        process = subprocess.Popen(command, stdout=subprocess.PIPE)
        numRowsCleared, numTurns = process.stdout.read().decode("utf-8").strip("\r\n").split(" ")
        totalRowsCleared += int(numRowsCleared)
        totalTurns += int(numTurns)
    return (totalRowsCleared // numAttempts, totalTurns // numAttempts)

class AlphaTetris():
    """Genetic Algorithm to optimise weights for heuristics in PlayerSkeleton.java"""

    pool_size = 8
    defaultWeights = [
        -0.510066, # Aggregate column heights
        -0.184483, # Bumpiness
        0, # Max height
        -0.6, # Num of holes created
        0.760666 # Num of completed rows
    ]

    # True if weight should be positive
    shouldWeightBePositive = [
        False,
        False,
        False,
        False,
        True
    ]

    def __init__(self):
        logging.basicConfig(filename='%s.log' % time.ctime().replace(" ","_").replace(":","-"), level=logging.INFO)
        logging.getLogger().addHandler(logging.StreamHandler())
        self.pool = Pool(AlphaTetris.pool_size)
        self.populationCount = 400    # Must be even
        self.chooseParentsRate = 0.1
        self.regenParentsRate = 0.05
        self.mutationRate = 0.25    # [0, 1]
        self.generation = 1
        self.start([AlphaTetris.defaultWeights] + [self.generateWeights() for i in range(self.populationCount - 1)])

    def start(self, populationWeights):
        logging.info("Generation " + str(self.generation))

        choices = []

        try:
            scores = self.pool.map(runGame, populationWeights)
        except KeyboardInterrupt as ki:
            self.pool.join()
            self.pool.terminate()
        else:
            totalRowsCleared = 0
            for (numRowsCleared, numTurns), weights in zip(scores, populationWeights):
                #print("Rows cleared: " + str(numRowsCleared) + "\tNumber of turns: " + str(numTurns) + "\tWeights used: " + str(weights))
                totalRowsCleared += numRowsCleared
                choices.append((weights, numRowsCleared))

            parents = self.selectParents(choices)
            children = self.crossover(parents)
            self.mutate(children)

            self.generation += 1
            logging.info("Average number of rows cleared: " + str(totalRowsCleared // self.populationCount))
            self.start(parents + children)

    #### Initial population
    def generateWeights(self, delta=0.01):
        return [self.generateWeight(idx, weight, delta) for idx, weight in enumerate(AlphaTetris.defaultWeights)]

    def generateWeight(self, baseWeightIndex, baseWeight, delta=0.1):
        # isPositive = AlphaTetris.shouldWeightBePositive[baseWeightIndex]

        # weight = random.gauss(baseWeight, min(0.3, (1 - abs(baseWeight))/2)) # TODO change standard deviation
        # if isPositive:   # Weight should be positive
        #     return abs(weight)
        # else:
        #     return -abs(weight)
        return baseWeight + random.uniform(-delta, delta)


    #### Selection
    def selectParents(self, choices):
        parentsNum = int(self.chooseParentsRate * self.populationCount)
        choices.sort(key=lambda choice: choice[1], reverse=True)
        parents = [choice[0] for choice in choices[:parentsNum]]
        for choice in choices[:5]:
            logging.info("Score: %s, Weights: %s" % (choice[1], choice[0]))
        return parents

    #### Crossover
    def crossover(self, parents):
        childrenNum = self.populationCount - len(parents)
        children = []
        for i in range(childrenNum):
            parent_weights = zip(*random.sample(parents, 2))
            children.append([random.choice(weight) for weight in parent_weights])
        return children


    #### Mutation
    def mutate(self, parents):
        for parent in parents:
            if random.uniform(0, 1) < self.mutationRate:
                mutation_target = random.randint(0, len(AlphaTetris.defaultWeights) - 1)
                parent[mutation_target] = self.generateWeight(mutation_target, parent[mutation_target])



if __name__ == '__main__':
    AlphaTetris()