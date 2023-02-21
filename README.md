# data-generator

数据生成服务

### 后置脚本处理

> 后置脚本处理目前支持两种方式：`javascript`和`spel`。

#### javascript

javascript脚本目前仅仅支持立即执行的脚本（即脚本引擎加载后可以直接运行脚本内容，而不需要进行调用）。如下所示：

```javascript
(context, dataset, args) => {
    var data = [];
    for (var i = 0; i < dataset.length; i++) {
        data.push(arr[i].CODE);
    }
    return data;
}
```

对于上述脚本内容的，有三个参数：`context`、`dataset`、`args`。

1. context 参数包含全局数据集，获取指定数据集方式为如下，其中`global_ds_1`为数据集编号。

```javascript
var ds1 = context.global('global_ds_1');
```

2. dataset 参数为当前脚本执行的数据集参数，通常来说，对于Reader上下文中该数据集通常为单一的数据集，即数组；对于表字段上下文中该数据集为对象，其中可能包含多个Reader的数据集；

```javascript
//Reader 中单一数据集则直接为数组对象，可以直接使用
//对于表字段上下文的数据集则为对象，可以根据数据集编号来获取对应Reader的数据集，如下所示
var arr = dataset['current_ds_2'];
```

3. args 参数为脚本执行的附加参数，目前为预留扩展；

#### spel
`SPEL`表达式目前支持两种方式：原生和自定义。