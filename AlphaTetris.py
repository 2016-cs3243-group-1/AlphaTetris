#!/usr/bin/python

import subprocess
import sys
import random
import signal
import logging
import time
import threading
from collections import Counter
from fractions import Fraction

# python 2.x/3.x specific checks
try:
    from Queue import Queue
except ImportError:
    from queue import Queue

try:
    xrange
except NameError:
    xrange = range

class GameWorker(threading.Thread):
    """
    Worker class for handling the calling of the java tetris game.
    Consumes work from the queue when available and returns
    the result through a callback method.
    """

    def __init__(self, lock, game_queue, running, callback):
        threading.Thread.__init__(self)
        self.daemon = True
        self.lock = lock
        self.game_queue = game_queue
        self.running = running
        self.callback = callback

    def _run_game(self, weights):
        command = ["java", "PlayerSkeleton"] + [str(x) for x in weights]
        process = subprocess.Popen(command, stdout=subprocess.PIPE)
        rows_cleared, turns = process.stdout.read().decode("utf-8").strip("\r\n").split(" ")
        return int(rows_cleared)

    def run(self):
        while self.running:
            idx, weights = self.game_queue.get()
            rows_cleared = self._run_game(weights)
            with self.lock:
                self.callback(idx, rows_cleared)
            self.game_queue.task_done()

class AlphaTetris():
    """Genetic Algorithm to optimise weights for heuristics in PlayerSkeleton.java"""

    workers = 8 #threads
    population_size = 500 # number of agents
    games = 50 # no of games per agent
    selection = 0.1 # random pool size to select best parents from
    culling = 0.3 # % of population to cull and replace every generation
    mutation_rate = 0.05 # mutation rate
    mutation_delta = 0.2 # % range of mutation adjustment

    num_weights = 5

    logging.basicConfig(filename='%s.log' % time.ctime().replace(" ","_").replace(":","-"), level=logging.INFO)
    logging.getLogger().addHandler(logging.StreamHandler())

    def __init__(self):
        self.queue = Queue()
        self.running = True
        self.lock = threading.Lock()
        self._spawn_workers()
        self.population = self._seed_population()

    def _worker_callback(self, idx, rows_cleared):
        self._results[idx] += rows_cleared
        self._total_rows_cleared += rows_cleared

    def _spawn_workers(self):
        """spawns the threaded worker class instances"""
        for x in xrange(self.workers):
            worker = GameWorker(self.lock, self.queue, self.running, self._worker_callback)
            worker.start()

    def _queue_games(self, population):
        """puts the work into the queue for the worker instances to consume"""
        [map(self.queue.put, enumerate(population)) for game in xrange(self.games)]

    def _normalize(self, weights):
        """normalize values to 1. if all weights are 0 return 0.5 (for crossover average weighted fitness)"""
        sum_weights = sum(weights)
        return map(lambda w: sum_weights > 0 and (float(w) / sum_weights) or 0.5, weights)

    def _generate_weights(self, result, remainder=1, n=1):
        """generates a random vector of length num_weights that sums to 1.0"""
        if n == self.num_weights:
            result.append(remainder)
            return result
        else:
            weight = random.uniform(0, remainder)
            result.append(weight)
            return self._generate_weights(result, remainder-weight, n+1)

    def _seed_population(self):
        """generates the initial population"""
        return [self._generate_weights([]) for x in xrange(self.population_size)]

    def _select_parents(self):
        """tournament selection"""
        random_selection = random.sample(xrange(self.population_size), int(self.population_size * 0.1))
        return sorted(random_selection, key=self._results.get, reverse=True)[:2]

    def _crossover(self, parent1, parent2):
        """average weighted crossover"""
        fitness1, fitness2 = self._normalize([self._results[parent1], self._results[parent2]]) 
        return [(fitness1 * p1) + (fitness2 * p2) for p1, p2 in zip(self.population[parent1], self.population[parent2])]

    def _mutate(self, offspring):
        """mutate randomly selected weight by delta and normalize"""
        weight_idx = random.choice(xrange(len(offspring)))
        mutation_modifier = random.uniform(-self.mutation_delta, self.mutation_delta)
        offspring[weight_idx] *= mutation_modifier
        return self._normalize(offspring)

    def _create_offspring(self):
        """create an offspring using tournament selection and average weighted crossover"""
        parents = self._select_parents()
        offspring = self._crossover(*parents)
        if (random.uniform(0, 1) < self.mutation_rate):
            self._mutate(offspring)
        return offspring
        
    def _next_generation(self, ranks):
        """cull the weakest population and replace them with new offspring"""
        replace = ranks[:int(self.population_size * self.culling)]
        for idx in replace:
            self.population[idx] = self._create_offspring()

    def _report(self, ranks):
        top5 = ranks[self.population_size-5:]
        for idx in top5[::-1]:
            logging.info("Rows Cleared: %s, Weights: %s" % (self._results[idx], self.population[idx]))
        logging.info("Total Rows Cleared: %s" % self._total_rows_cleared)


    def optimize_weights(self, generations):
        for gen in xrange(generations):
            logging.info(" Generation: %s" % gen)
            self._results = Counter()
            self._total_rows_cleared = 0
            self._queue_games(self.population)
            self.queue.join()

            ranks = sorted(xrange(self.population_size), key=self._results.get)
            self._report(ranks)
            self._next_generation(ranks)

if __name__ == '__main__':
    ap = AlphaTetris()
    ap.optimize_weights(100) 