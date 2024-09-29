(context, dataset, arg) => {
    console.log(arg);
    console.log(context.global())
    console.log(dataset['current_ds_2'])
    var arr = dataset['current_ds_2'];
    var data = [];
    for (var i = 0; i < arr.length; i++) {
        console.log(arr[i]);
        console.log(arr[i].CODE);
        data.push(arr[i].CODE);
    }
    console.log(data);
    return data;
}