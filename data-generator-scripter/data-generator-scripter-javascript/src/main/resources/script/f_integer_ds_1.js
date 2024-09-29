(function (context, dataset, args) {
    var data = [];
    for (var i = 0; i < dataset.length; i++) {
        data.push(dataset[i].CODE);
    }
    return data;
})