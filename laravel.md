```
php artisan make:model Event --migration

php artisan make:migration add_enabled_to_events_table --table=events

php artisan make:seed EventTableSeeder

php artisan tinker

php artisan migrate:rollback

php artisan make:controller EventsController --resource #resource means crud
```

```
////分页

php artisan vendor:publish --tag=laravel-pagination

$events = Events::paginate(10);

{!! $events->links('vendor.pagination.bootstrap-4') !!}

$events = Event::simplePaginate(10);

{!! $events->links('vendor.pagination.simple-bootstrap-4') !!}


php artisan make:controller EventsController --model=Event # 直接把event这个model当成参数

php artisan make:model Event --controller --resource --migration


//查询

$eventExists = Event::where('city' , 'Dublin')->exists();

//命名约定

setAttributeNameAttribute

getAttributeNameAttribute

//查询scope
local

global
```
