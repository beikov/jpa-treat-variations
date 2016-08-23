<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:exslt="http://exslt.org/common">
  <xsl:output method="text" indent="no"/>
  <!-- Remove spaces -->
  <xsl:strip-space elements="*"/>
  <!-- Kill default output -->
  <xsl:template match="text()"/>
  <!-- Test names in order -->
  <xsl:variable name="rootSelectTestNames">
    <test prefix="selectTreatedRootBasic" suffix=""/>
    <test prefix="selectMultipleTreatedRootBasic" suffix=""/>
    <test prefix="selectTreatedParentRootBasic" suffix=""/>
    <test prefix="selectMultipleTreatedParentRootBasic" suffix=""/>
    <test prefix="selectTreatedRootEmbeddableBasic" suffix=""/>
    <test prefix="selectMultipleTreatedRootEmbeddableBasic" suffix=""/>
    <test prefix="selectTreatedParentRootEmbeddableBasic" suffix=""/>
    <test prefix="selectMultipleTreatedParentRootEmbeddableBasic" suffix=""/>
  </xsl:variable>
  <xsl:variable name="rootWhereTestNames">
    <test prefix="whereTreatedRootBasic" suffix=""/>
    <test prefix="whereMultipleTreatedRootBasic" suffix=""/>
    <test prefix="whereTreatedRootEmbeddableBasic" suffix=""/>
    <test prefix="whereMultipleTreatedRootEmbeddableBasic" suffix=""/>
  </xsl:variable>
  <xsl:variable name="selectTestNames">
    <test prefix="selectTreated" suffix=""/>
    <test prefix="selectMultipleTreated" suffix=""/>
    <test prefix="selectTreatedParent" suffix=""/>
    <test prefix="selectMultipleTreatedParent" suffix=""/>
    <test prefix="selectTreatedEmbeddable" suffix=""/>
    <test prefix="selectMultipleTreatedEmbeddable" suffix=""/>
    <test prefix="selectTreatedParentEmbeddable" suffix=""/>
    <test prefix="selectMultipleTreatedParentEmbeddable" suffix=""/>
    <test prefix="selectTreatedEmbeddable" suffix="Embeddable"/>
    <test prefix="selectMultipleTreatedEmbeddable" suffix="Embeddable"/>
    <test prefix="selectTreatedParentEmbeddable" suffix="Embeddable"/>
    <test prefix="selectMultipleTreatedParentEmbeddable" suffix="Embeddable"/>
    <test prefix="selectTreatedRoot" suffix=""/>
    <test prefix="selectMultipleTreatedRoot" suffix=""/>
    <test prefix="selectTreatedParentRoot" suffix=""/>
    <test prefix="selectMultipleTreatedParentRoot" suffix=""/>
    <test prefix="selectTreatedRootEmbeddable" suffix=""/>
    <test prefix="selectMultipleTreatedRootEmbeddable" suffix=""/>
    <test prefix="selectTreatedParentRootEmbeddable" suffix=""/>
    <test prefix="selectMultipleTreatedParentRootEmbeddable" suffix=""/>
    <test prefix="selectTreatedRootEmbeddable" suffix="Embeddable"/>
    <test prefix="selectMultipleTreatedRootEmbeddable" suffix="Embeddable"/>
    <test prefix="selectTreatedParentRootEmbeddable" suffix="Embeddable"/>
    <test prefix="selectMultipleTreatedParentRootEmbeddable" suffix="Embeddable"/>
  </xsl:variable>
  <xsl:variable name="joinTestNames">
    <test prefix="treatJoin" suffix=""/>
    <test prefix="treatJoinMultiple" suffix=""/>
    <test prefix="treatJoinParent" suffix=""/>
    <test prefix="treatJoinMultipleParent" suffix=""/>
    <test prefix="treatJoinEmbeddable" suffix=""/>
    <test prefix="treatJoinMultipleEmbeddable" suffix=""/>
    <test prefix="treatJoinParentEmbeddable" suffix=""/>
    <test prefix="treatJoinMultipleParentEmbeddable" suffix=""/>
    <test prefix="joinTreatedRoot" suffix=""/>
    <test prefix="joinMultipleTreatedRoot" suffix=""/>
    <test prefix="joinTreatedParentRoot" suffix=""/>
    <test prefix="joinMultipleTreatedParentRoot" suffix=""/>
    <test prefix="joinTreatedRootEmbeddable" suffix=""/>
    <test prefix="joinMultipleTreatedRootEmbeddable" suffix=""/>
    <test prefix="joinTreatedParentRootEmbeddable" suffix=""/>
    <test prefix="joinMultipleTreatedParentRootEmbeddable" suffix=""/>
    <test prefix="treatJoinTreatedRoot" suffix=""/>
    <test prefix="treatJoinMultipleTreatedRoot" suffix=""/>
    <test prefix="treatJoinTreatedParentRoot" suffix=""/>
    <test prefix="treatJoinMultipleTreatedParentRoot" suffix=""/>
    <test prefix="treatJoinTreatedRootEmbeddable" suffix=""/>
    <test prefix="treatJoinMultipleTreatedRootEmbeddable" suffix=""/>
    <test prefix="treatJoinTreatedParentRootEmbeddable" suffix=""/>
    <test prefix="treatJoinMultipleTreatedParentRootEmbeddable" suffix=""/>
  </xsl:variable>
  
  <xsl:variable name="whitespace" select="'                                                                   '" />
  
  <xsl:template match="testsuites">
    <!-- Root treat -->
    <xsl:text>
==== Root treat
</xsl:text>
    <xsl:call-template name="RootResults" />
    
    <!-- Association treat -->
    <xsl:text>
    
==== Association treat
</xsl:text>
    <xsl:call-template name="AssociationResults">
        <xsl:with-param name="name" select="'Joined'"/>
        <xsl:with-param name="suffix" select="'[0]'"/>
    </xsl:call-template>
    <xsl:call-template name="AssociationResults">
        <xsl:with-param name="name" select="'SingleTable'"/>
        <xsl:with-param name="suffix" select="'[1]'"/>
    </xsl:call-template>
    <xsl:call-template name="AssociationResults">
        <xsl:with-param name="name" select="'TablePerClass'"/>
        <xsl:with-param name="suffix" select="'[2]'"/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template name="RootResults">
      <xsl:text>
[cols="e,^,^,^"]
|=================================================================================================================================================
|Name                                                               | Joined                  | SingleTable             | TablePerClass
|**SELECT**                                                         |                         |                         |
</xsl:text>
    <xsl:for-each select="exslt:node-set($rootSelectTestNames)/test">
        <xsl:call-template name="RootRow">
            <xsl:with-param name="prefix" select="@prefix"/>
            <xsl:with-param name="suffix" select="@suffix"/>
        </xsl:call-template>
    </xsl:for-each>
    <xsl:text>|**WHERE**                                                          |                         |                         |
</xsl:text>
    <xsl:for-each select="exslt:node-set($rootWhereTestNames)/test">
        <xsl:call-template name="RootRow">
            <xsl:with-param name="prefix" select="@prefix"/>
            <xsl:with-param name="suffix" select="@suffix"/>
        </xsl:call-template>
    </xsl:for-each>
    <xsl:text>|=================================================================================================================================================
</xsl:text>
  </xsl:template>

  <xsl:template name="RootRow">
    <xsl:param name="prefix" />
    <xsl:param name="suffix" />
    <xsl:variable name="joinedName" select="concat($prefix, $suffix, '[0]')" />
    <xsl:variable name="singleTableName" select="concat($prefix, $suffix, '[1]')" />
    <xsl:variable name="tablePerClassName" select="concat($prefix, $suffix, '[2]')" />
    <!-- Right-pad whitespaces -->
    <xsl:text>|</xsl:text>
    <xsl:value-of select="substring(concat(' ', $prefix, $suffix, $whitespace), 1, 67)" />
    <xsl:text>|</xsl:text>
    <xsl:call-template name="TestResult">
        <xsl:with-param name="testName" select="$joinedName"/>
    </xsl:call-template>
    <xsl:text>|</xsl:text>
    <xsl:call-template name="TestResult">
        <xsl:with-param name="testName" select="$singleTableName"/>
    </xsl:call-template>
    <xsl:text>|</xsl:text>
    <xsl:call-template name="TestResult">
        <xsl:with-param name="testName" select="$tablePerClassName"/>
    </xsl:call-template>
    <xsl:text>
</xsl:text>
  </xsl:template>
  
  <xsl:template name="AssociationResults">
    <xsl:param name="name" />
    <xsl:param name="suffix" />
    <xsl:text>
===== </xsl:text><xsl:value-of select="$name"/><xsl:text>

[cols="e,^,^,^,^,^"]
|=====================================================================================================================================================================================================
|Name                                                               | ManyToOne               | OneToManyList           | OneToManyInverseSet     | ManyToManyMapKey        | ManyToManyMapValue
|**SELECT**                                                         |                         |                         |                         |                         |                    
</xsl:text>
    <xsl:for-each select="exslt:node-set($selectTestNames)/test">
        <xsl:call-template name="TestRow">
            <xsl:with-param name="prefix" select="@prefix"/>
            <xsl:with-param name="suffix" select="@suffix"/>
            <xsl:with-param name="strategySuffix" select="$suffix"/>
        </xsl:call-template>
    </xsl:for-each>
    <xsl:text>|**JOIN**                                                           |                         |                         |                         |                         |                    
</xsl:text>
    <xsl:for-each select="exslt:node-set($joinTestNames)/test">
        <xsl:call-template name="TestRow">
            <xsl:with-param name="prefix" select="@prefix"/>
            <xsl:with-param name="suffix" select="@suffix"/>
            <xsl:with-param name="strategySuffix" select="$suffix"/>
        </xsl:call-template>
    </xsl:for-each>
    <xsl:text>|=====================================================================================================================================================================================================
</xsl:text>
  </xsl:template>

  <xsl:template name="TestRow">
    <xsl:param name="prefix" />
    <xsl:param name="suffix" />
    <xsl:param name="strategySuffix" />
    <xsl:variable name="manyToOneName"           select="concat($prefix, 'ManyToOne', $suffix, $strategySuffix)"/>
    <xsl:variable name="oneToManyListName"       select="concat($prefix, 'OneToManyList', $suffix, $strategySuffix)"/>
    <xsl:variable name="oneToManyInverseSetName" select="concat($prefix, 'OneToManyInverseSet', $suffix, $strategySuffix)"/>
    <xsl:variable name="manyToManyMapKeyName"    select="concat($prefix, 'ManyToManyMapKey', $suffix, $strategySuffix)"/>
    <xsl:variable name="manyToManyMapValueName"  select="concat($prefix, 'ManyToManyMapValue', $suffix, $strategySuffix)"/>
    <!-- Right-pad whitespaces -->
    <xsl:text>|</xsl:text>
    <xsl:value-of select="substring(concat(' ', $prefix, '{Association}', $suffix, $whitespace), 1, 67)" />
    <xsl:text>|</xsl:text>
    <xsl:call-template name="TestResult">
        <xsl:with-param name="testName" select="$manyToOneName"/>
    </xsl:call-template>
    <xsl:text>|</xsl:text>
    <xsl:call-template name="TestResult">
        <xsl:with-param name="testName" select="$oneToManyListName"/>
    </xsl:call-template>
    <xsl:text>|</xsl:text>
    <xsl:call-template name="TestResult">
        <xsl:with-param name="testName" select="$oneToManyInverseSetName"/>
    </xsl:call-template>
    <xsl:text>|</xsl:text>
    <xsl:call-template name="TestResult">
        <xsl:with-param name="testName" select="$manyToManyMapKeyName"/>
    </xsl:call-template>
    <xsl:text>|</xsl:text>
    <xsl:call-template name="TestResult">
        <xsl:with-param name="testName" select="$manyToManyMapValueName"/>
    </xsl:call-template>
    <xsl:text>
</xsl:text>
  </xsl:template>
  
  <xsl:template name="TestResult">
    <xsl:param name="testName" />
    <xsl:variable name="errorMessage" select="//testcase[@name = $testName]/failure/text() | //testcase[@name = $testName]/error/text()"/>
    <xsl:variable name="errorReason" select="1"/>
    <xsl:choose>
      <xsl:when test="$errorMessage"><xsl:value-of select="substring(concat(':fail:', $whitespace), 1, 25)" /></xsl:when>
      <xsl:otherwise>
          <xsl:value-of select="substring(concat(':pass:', $whitespace), 1, 25)" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="string-replace-all">
    <xsl:param name="text" />
    <xsl:param name="replace" />
    <xsl:param name="by" />
    <xsl:choose>
        <xsl:when test="$text = '' or $replace = ''or not($replace)" >
            <!-- Prevent this routine from hanging -->
            <xsl:value-of select="$text" />
        </xsl:when>
        <xsl:when test="contains($text, $replace)">
            <xsl:value-of select="substring-before($text,$replace)" />
            <xsl:value-of select="$by" />
            <xsl:call-template name="string-replace-all">
                <xsl:with-param name="text" select="substring-after($text,$replace)" />
                <xsl:with-param name="replace" select="$replace" />
                <xsl:with-param name="by" select="$by" />
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$text" />
        </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>