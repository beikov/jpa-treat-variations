# Treat variations


This is a collection of test cases for the treat operator.

It assumes some very specific behavior of the treat operator which might even differ from the JPA spec or intuition at first.
In addition to that, the following also tests for edge cases the JPA spec seemed to have forgotten to include.

There are 2 types of treat expressions
 - Root path treats e.g. "TREAT(rootAlias AS Subtype)"
 - Subpath treats e.g. "TREAT(rootAlias.subpath AS Subtype)"

Treat expressions can be used in different forms in different clauses
 - In SELECT/WHERE/ORDER BY all types of treat expressions must be followed by a subpath e.g. "TREAT(rootAlias AS Subtype).subpath"
 - In FROM a treat expression must be one of the following
   - Subpath treat e.g. "TREAT(rootAlias.subpath AS Subtype)"
   - Subpath treat with root path treat e.g. "TREAT(TREAT(rootAlias AS Subtype).subpath AS Subtype)"
   - Root path treat followed by subpath e.g. "TREAT(rootAlias AS Subtype).subpath"

## Simplification translation strategy

S1. Nested treat expressions in non-FROM clauses can be implemented by replacing subexpressions with join nodes.
An expression like "TREAT(TREAT(rootAlias AS Subtype).subpath AS Subtype).subpath" will emmit "INNER JOIN TREAT(rootAlias AS Subtype).subpath p"
and the expression becomes "TREAT(p AS Subtype).subpath"

S2. Subpath treats in non-FROM clauses follow JPA navigation semantics and can be simplified by emmiting an inner join for the treated path 
and replacing the path with the join alias e.g. "TREAT(rootAlias.subpath AS Subtype).subpath" will emmit "INNER JOIN rootAlias.subpath p" 
and the expression becomes "TREAT(p AS Subtype).subpath"

S3. Since queries for types that have subtypes should produce concrete subtype values e.g. "SELECT o FROM Supertype o" returns [Subtype1, Subtype2, ...], subtype relations must be semantically left joined.
A treat join like "LEFT JOIN TREAT(root.subpath Subtype) p" can be simplified to "LEFT JOIN root.subpath p ON TYPE(p) = Subtype" and replacing every usage of "p" with "TREAT(p AS Subtype)"

S4. The join type for a relation join must semantically cascade for joining the type hierarchy of the relation, 
meaning that e.g. "LEFT JOIN TREAT(root.subpath Subtype)" must use left join semantics not only for joining the subpath relation(might be collection table) 
and the concrete subtype relation, but also for joining the supertype hierarchy relations.
The on clause predicates must be put on the appropriate relation joins on clauses so that the "illusion" of joining against a single table is kept(might require predicates to be transformed because "ON KEY(...) = ..." might get tricky)

S5. Due to this possible translation strategy the only new feature that needs deep implementation support is a root path treat.
When translating to SQL, the root path treat must be translated to the alias of the proper subtype relation.

## Possible optimizations

O1. The optimization of avoiding joins for subtype relations can actually be seen independent of treat support when applying the translation strategy.
When encountering a top-level equality- or in-predicate of the form "TYPE(fromAlias) = Subtype" or "TYPE(fromAlias) IN (Subtype, Subtype2)" in a WHERE or ON clause, 
then the type hierarchy joins for the "fromAlias" relation can be reduced to only include the listed subtype relations.
Since the translation strategy mandates translating a treat join like "JOIN TREAT(rootAlias.subpath AS Subtype) p" to "JOIN rootAlias.subpath p ON TYPE(p) = Subtype",
joining other subrelations can be avoided in this case.

O2. Another possible optimization is to avoid joins for super types if no super type properties are used.

O3. If it can be proven, that for top-level predicates of the form "TREAT(root AS Subtype).subpath OPERATOR ..." the predicate is always false for a subtype T1, 
then the subtype mustn't be joined as instances of that type would be filtered out anyway.


In summary, treat joins can be reduced to normal joins with type restrictions and by replacing join aliases with the treated version.
Subpath treats can be replaced with root path treats by adding inner joins for paths. Treats in general do not cause subtype filtering,
only the type restrictions do.

## Findings so far

Eclipselink

 - does not support map in embeddables(BUGREPORT!)
 - has no support for treat in conjunction with table per class
 - wrongly assumes it can filter when encountering a treat
 - does not support root path treat in FROM

&nbsp;

Hibernate

 - has problems with same name properties in inheritance hierarchies: https://hibernate.atlassian.net/browse/HHH-11037
 - only supports treat in the FROM clause: https://hibernate.atlassian.net/browse/HHH-10988
 - doesn't properly optimize unnecessary joins away: https://hibernate.atlassian.net/browse/HHH-10887

&nbsp;

Datanucleus 

 - does not seem to support the TablePerClass mappings: https://github.com/datanucleus/datanucleus-rdbms/issues/89

## Test overview
 
The following should illustrate which test, tests which expression. Every test comes with a "multiple" counterpart to test a query with the expression with 2 different subtypes.

### Root treat

Name                                                            |Expression
------------                                                    |-------------
**SELECT**                                                      |
   selectTreatedRootBasic:                                      |TREAT(root).property
   selectTreatedParentRootBasic:                                |TREAT(parentRoot).property
   selectTreatedRootEmbeddableBasic:                            |TREAT(root).embeddable.property
   selectTreatedParentRootEmbeddableBasic:                      |TREAT(parentRoot).embeddable.property
**WHERE**                                                       |
   whereTreatedRootBasic:                                       |TREAT(root).property
   whereTreatedRootEmbeddableBasic:                             |TREAT(root).embeddable.property


### Association treat

Possible associations
 * ManyToOne
 * OneToManyList
 * OneToManyInverseSet
 * ManyToManyMapKey e.g. KEY(association)
 * ManyToManyMapValue e.g. VALUE(association) or simply association


Name                                                            |Expression
------------                                                    |-------------
**SELECT**                                                      |
   selectTreated{Association}:                                  |TREAT(root.association).property
   selectTreatedParent{Association}:                            |TREAT(parentRoot.association).property
   selectTreatedEmbeddable{Association}:                        |TREAT(root.embeddable.association).property
   selectTreatedParentEmbeddable{Association}:                  |TREAT(parentRoot.embeddable.association).property
   selectTreatedEmbeddable{Association}Embeddable:              |TREAT(root.embeddable.association).embeddable.property
   selectTreatedParentEmbeddable{Association}Embeddable:        |TREAT(parentRoot.embeddable.association).embeddable.property
   selectTreatedRoot{Association}:                              |TREAT(TREAT(root).association).property
   selectTreatedParentRoot{Association}:                        |TREAT(TREAT(parentRoot).association).property
   selectTreatedRootEmbeddable{Association}:                    |TREAT(TREAT(root).embeddable.association).property
   selectTreatedParentRootEmbeddable{Association}:              |TREAT(TREAT(parentRoot).embeddable.association).property
   selectTreatedRootEmbeddable{Association}Embeddable:          |TREAT(TREAT(root).embeddable.association).embeddable.property
   selectTreatedParentRootEmbeddable{Association}Embeddable:    |TREAT(TREAT(parentRoot).embeddable.association).embeddable.property
**JOIN**                                                        |
   treatJoin{Association}:                                      |TREAT(root.association)
   treatJoinParent{Association}:                                |TREAT(parentRoot.association)
   treatJoinEmbeddable{Association}:                            |TREAT(root.embeddable.association)
   treatJoinParentEmbeddable{Association}:                      |TREAT(parentRoot.embeddable.association)
   joinTreatedRoot{Association}:                                |TREAT(root).association
   joinTreatedParentRoot{Association}:                          |TREAT(parentRoot).association
   joinTreatedRootEmbeddable{Association}:                      |TREAT(root).embeddable.association
   joinTreatedParentRootEmbeddable{Association}:                |TREAT(parentRoot).embeddable.association
   treatJoinTreatedRoot{Association}:                           |TREAT(TREAT(root).association)
   treatJoinTreatedParentRoot{Association}:                     |TREAT(TREAT(parentRoot).association)
   treatJoinTreatedRootEmbeddable{Association}:                 |TREAT(TREAT(root).embeddable.association)
   treatJoinTreatedParentRootEmbeddable{Association}:           |TREAT(TREAT(parentRoot).embeddable.association)