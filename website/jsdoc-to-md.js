'use strict'
const jsdoc2md = require('jsdoc-to-markdown')
const fs = require('fs')
const path = require('path')

/* input and output paths */
const inputFiles = [
  '../react-native-cbl.js',
  //'../cbl-provider-decorator.js',
].map( f => path.resolve(__dirname, f) )
const outputDir = path.resolve(__dirname, '../docs')

inputFiles.forEach( f => {
  const templateData = jsdoc2md.getTemplateDataSync({ files: f })
  const template = templateData[0].kind === 'function'
? `{{#function name="${templateData[0].name}"}}
---
title: ${templateData[0].name}
---
{{>docs}}
{{/function}}
`
: `{{#class name="${templateData[0].name}"}}
---
title: ${templateData[0].name}
---
{{>members~}}
{{/class}}
`
  const output = jsdoc2md.renderSync({
    data: templateData,
    template: template,
    'param-list-format': 'list',
    //partial: ['partials/main.hbs'].map( f => path.resolve(__dirname, f) )
  })
  fs.writeFileSync(path.resolve(outputDir, `api-${path.parse(f).name}.md`), output)
})
