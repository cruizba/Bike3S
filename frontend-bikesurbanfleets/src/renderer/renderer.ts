import 'core-js/es6';
import 'core-js/es7/reflect';
import 'zone.js/dist/zone'; // if any issues arise use zone-mix instead
import 'rxjs';
import 'rxjs/observable/IntervalObservable';
import 'rxjs/BehaviorSubject';
import 'rxjs/ReplaySubject';

import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import './css';
import 'conic-gradient/conic-gradient';
import './styles.css';

if (process.env.target === 'production') {
    enableProdMode();
} else {
    // Development and test
    Error['stackTraceLimit'] = Infinity;
    require('zone.js/dist/long-stack-trace-zone');
}

platformBrowserDynamic().bootstrapModule(AppModule);
