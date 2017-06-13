#Rcr2

## Goal

Demonstrate a program that can perform better than random without explicitly 
programming it to do so.

Method is to record steps of an "imitated" (human) and based on biases 
developed in observations of the imitated, apply those biases to automated
steps of the "imitator" (program runtime).

## Macro steps

1. input text 
2. parse executable parts of text from non-executable
3. execute executable parts
4. determine best action based on decision logic of non-executable parts
    * Micro steps here
3. transform best action into new text or script
4. apply new text to 1

## Persistence

Persistence stores map a previous state to the next action and its result

```
{
  key: State,
  next: {
    [action]: FeedbackStats
  }
}
```

State is a function of what environment variables are available in working memory. Working memory looks like this:

Step | Alias | Code | State Serialization
--- | --- | --- | --- 
 t | t | NA | t
 3 | A | A = fn1 text | t:fn1 t
 2 | B | B = fn2 A | t:fn1 t:fn2 (fn1 t)
 1 | C | C = fn2 B | t:fn1 t:fn2 (fn1 t):fn2 (fn2 (fn1 t))
 0 | D | D = fn3 A B | t:fn1 t:fn2 (fn1 t):fn2 (fn2 (fn1 t)):fn3 (fn2 (fn1 t)) (fn2 (fn2 (fn1 t))) 

We can map working memory to a state using a trie structure

```
0 t
1 A 
2 B
3 C D
```

Any next step we take can depend on any parent state in the tree. Therefore to find the next
step we can first consider persisted stats from the 3rd tree level, then if those samples
are insufficient, we consider those from the 2nd tree level, and so on until we are
confident with a next step.

However, note that we could also condense the tree from the other direction:

```
0 t
1 B'
2 C' D'
```

Where `B'` is a transform of `B` such that any component of `B` depending on A is expanded
with any other dependent available in memory. For example if

```
0 t
...
X Y Z # given we have X,Y,Z in memory
...
1 B' # <= B(A -> t,X,Y,Z) we can expand B to replace A with any dependency in t,X,Y,Z
2 C' D' # and similarly here with C', D'
```

In this way we can actually abstract in two different directions. Use this data structure

```
                        Tree 1 Tree 2
    After   Before      Node    Node
1   [A,B,D] -> []      -> A     X
2   [B,D]   -> [A]     -> B     B
3   [D]     -> [A,B]   -> D
...                             
4   [D]     -> [X,B]   ->       D
```

If we were at node `D` we could then look at the related After node.
After that we could replace the dependency on A with a different dependency
in working memory, `X`, yielding `[X,B]`. All nodes pointing back to `[X,B]`
under `[D]` are then abstractly related. Their statistics can be combined
with those from the original node.
 

### Feedback

Feedback is generated in two phases

1. __Upon execution of a failing statement__

Imagine we have the following scenario

Step | Alias | Code | State Serialization
--- | --- | --- | --- 
 t | t | NA | t
 0 | A | A = fn1 text | t:fn1 t
 1 | B | B = fn2 A | t:fn1 t:fn2 (fn1 t)
 
if `fn2 A failed` then we can 

# Examples

## Cells
 
## Object recognition
 
Text is an image that can be interfaced with as a 2d array. Basic functions are those 
that parse objects out of this image by determining continuous areas for which some 
property of the array's entries is not changing. Imitated will then make assertions 
about the relationship between different objects, and from these relationships 
assert the presence of other objects. The goal is to develop a logic for recognition
imitating human understanding of salient patterns.

This opens up the idea of atoms in Rcr2. Unlike atoms in other logic oriented languages,
atoms here are stochastic. Here's an example.

```
# find a "face" in the image 
p = patterns text;
c = circles p;
oc = outmost c;
ic = inside oc c;
u = curves p;
ui = inside oc u;
m =  below ic ui;
# assert that there is a face if  
# m and ic are truthy 
atom face m ic;
```

We now have at least one definition for the atom `face`. There could be other definitions
though. How do we resolve a single "best" definition?

The answer is through feedback. When in a session an imitated makes a call for an atom,
we actually will run the statements the atom depends on.

```
f = face text;
```

Any subsequent statements depending on `f` may now fail, and their feedback will be applied
to f. Similarly, a side effect may succeed and its feedback will be applied to f. In this way
the actual definition called is the _current best_ definition, but it may change in the future. 