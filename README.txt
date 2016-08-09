Treat variations
======================

This collection of test cases tries to cover all possible cases of the treat operator.
The following should illustrate which test, tests which expression. Every test comes with a "multiple" counterpart to test a query with the expression with 2 different subtypes.

-------------------------------------------------------------
                       Root treat
-------------------------------------------------------------

SELECT
 - selectTreatedRootBasic:                                       TREAT(root).property
 - selectTreatedParentRootBasic:                                 TREAT(parentRoot).property
 
 - selectTreatedRootEmbeddableBasic:                             TREAT(root).embeddable.property
 - selectTreatedParentRootEmbeddableBasic:                       TREAT(parentRoot).embeddable.property

WHERE
 - whereTreatedRootBasic:                                        TREAT(root).property
 - whereTreatedRootEmbeddableBasic:                              TREAT(root).embeddable.property

 -------------------------------------------------------------
                     Association treat
-------------------------------------------------------------

Possible associations
 * ManyToOne
 * OneToManyList
 * OneToManyInverseSet
 * ManyToManyMapKey e.g. KEY(association)
 * ManyToManyMapValue e.g. VALUE(association) or simply association

SELECT
 - selectTreated{Association}:                                   TREAT(root.association).property
 - selectTreatedParent{Association}:                             TREAT(parentRoot.association).property
 
 - selectTreatedEmbeddable{Association}:                         TREAT(root.embeddable.association).property
 - selectTreatedParentEmbeddable{Association}:                   TREAT(parentRoot.embeddable.association).property
 
 - selectTreatedEmbeddable{Association}Embeddable:               TREAT(root.embeddable.association).embeddable.property
 - selectTreatedParentEmbeddable{Association}Embeddable:         TREAT(parentRoot.embeddable.association).embeddable.property
 
 - selectTreatedRoot{Association}:                               TREAT(TREAT(root).association).property
 - selectTreatedParentRoot{Association}:                         TREAT(TREAT(parentRoot).association).property

 - selectTreatedRootEmbeddable{Association}:                     TREAT(TREAT(root).embeddable.association).property
 - selectTreatedParentRootEmbeddable{Association}:               TREAT(TREAT(parentRoot).embeddable.association).property
 
 - selectTreatedRootEmbeddable{Association}Embeddable:           TREAT(TREAT(root).embeddable.association).embeddable.property
 - selectTreatedParentRootEmbeddable{Association}Embeddable:     TREAT(TREAT(parentRoot).embeddable.association).embeddable.property

JOIN
 - treatJoin{Association}:                                       TREAT(root.association)
 - treatJoinParent{Association}:                                 TREAT(parentRoot.association)
 
 - treatJoinEmbeddable{Association}:                             TREAT(root.embeddable.association)
 - treatJoinParentEmbeddable{Association}:                       TREAT(parentRoot.embeddable.association)
 
 - joinTreatedRoot{Association}:                                 TREAT(root).association
 - joinTreatedParentRoot{Association}:                           TREAT(parentRoot).association
 
 - joinTreatedRootEmbeddable{Association}:                       TREAT(root).embeddable.association
 - joinTreatedParentRootEmbeddable{Association}:                 TREAT(parentRoot).embeddable.association
 
 - treatJoinTreatedRoot{Association}:                            TREAT(TREAT(root).association)
 - treatJoinTreatedParentRoot{Association}:                      TREAT(TREAT(parentRoot).association)
 
 - treatJoinTreatedRootEmbeddable{Association}:                  TREAT(TREAT(root).embeddable.association)
 - treatJoinTreatedParentRootEmbeddable{Association}:            TREAT(TREAT(parentRoot).embeddable.association)