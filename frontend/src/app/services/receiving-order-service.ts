import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { ReceivingOrder } from '../models/receiving-order.model';

@Service()
export class ReceivingOrderService {
    private httpClient = inject(HttpClient)
    private url = 'http://localhost:8080/api/receiving';

    getReceivingOrder(orderId: string): Observable<ReceivingOrder> {
        return this.httpClient.get<ReceivingOrder>( `${this.url}/${orderId}`)
    }

    addArticleQuantity(orderId: string, articleId:string, quantity:number){
        console.log(`${this.url}/${orderId}/${articleId}/${quantity}`)
        return this.httpClient.put<ReceivingOrder>(`${this.url}/${orderId}/${articleId}/${quantity}`, {
            // quantity:quantity
        })
    }

    isOrderFinished(orderId: string) {
        return this.httpClient.get<ReceivingOrder>( `${this.url}/${orderId}/check`)
    }
}
