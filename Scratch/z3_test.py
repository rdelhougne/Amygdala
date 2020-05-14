#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# This is a small test file for fiddling with Z3 Python API

import z3

z3_version = z3.get_version()
print(f"Using Z3 Version: {z3_version[0]}.{z3_version[1]}.{z3_version[2]}")
assert z3.get_version() >= (4, 8, 6, 0)
z3.set_option('smt.string_solver', 'z3str3')
z3.set_option('timeout', 30 * 1000)

zn = z3.Int('n')
z3.solve(z3.Not(zn < 0))

x = z3.Real('x')
eqn = (2 * x**2 - 11 * x + 5 == 0)
z3.solve(eqn)
