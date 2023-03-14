<h1> <img align="left" width="50" height="50" src="https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/static/intellij-deodorant/icon.svg" alt="IntelliJDeodorant Icon"> IntelliJDeodorant </h1>


一种基于类级网路的软件系统半自动化重构方法IDEA实现插件

<p align="center">
  <img src="https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/static/intellij-deodorant/long-method.gif" width="90%"/>
</p>

## Supported Refactoring Dimensions
本插件目前支持两种重构维度：高内聚低耦合、代码可读性

- **High Cohesion Low Coupling** 提高代码内聚程度同时降低耦合程度的重构

- **Code Readablity** 提高代码可读性的重构

## Getting started
安装好插件后，IntelliJ IDEA界面右下角会显示```IntelliRefactor```按钮。点击按钮即进入工具窗口，此窗口中每个重构维度都包含一个```Refresh```按钮，用于生成该重构维度下的可执行重构操作。要应用任何重构操作，只需选中列表中的操作，然后单击```Refactor```按钮。
